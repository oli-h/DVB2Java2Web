package ch.oli.web;

import ch.oli.decode.PacketReader;
import ch.oli.ioctl.C;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;

@Service
public class DecoderNIT {

    @Autowired
    private ServerWebSocketHandler serverWebSocketHandler;

    public void decode(PacketReader prSection, int table_id, int something) {
        int network_id = something;
        if (network_id != 43020) {
            return;
        }

//        System.out.printf(" NIT: %s network_id=%d", table_id == 0x40 ? "actual" : "other ", network_id);

        {
            int network_descriptors_length = prSection.pull16() & 0xFFF;
            PacketReader prDescriptors = prSection.nextBytesAsPR(network_descriptors_length);
            while (prDescriptors.hasBytes()) {
                int descriptor_tag = prDescriptors.pull8();
                int descriptor_length = prDescriptors.pull8();
                PacketReader prDescriptor = prDescriptors.nextBytesAsPR(descriptor_length);
                if (descriptor_tag == 0x40) { // 0x40=network_name_descriptor
                    String network_name = prDescriptor.pullChar(descriptor_length);
                } else if (descriptor_tag == 0x4a) { // linkage_descriptor
                } else if (descriptor_tag == 0x5f) { // private_data_specifier_descriptor
                } else if (descriptor_tag == 0x84) { // ???
                } else if (descriptor_tag == 0x87) { // ???
                } else if (descriptor_tag == 0x89) { // ???
                } else if (descriptor_tag == 0xd2) { // ???
                } else {
                }
                // also seen (decimal): 74
            }
        }

        {
            int transport_stream_loop_length = prSection.pull16() & 0xFFF;
            PacketReader prTransportStreams = prSection.nextBytesAsPR(transport_stream_loop_length);
            while (prTransportStreams.hasBytes()) {
                TransportStream ts = new TransportStream();
                ts.network_id = network_id;
                ts.transport_stream_id = prTransportStreams.pull16();
                ts.original_network_id = prTransportStreams.pull16();
                int transport_descriptors_length = prTransportStreams.pull16() & 0xFFF;
                PacketReader prDescriptors = prTransportStreams.nextBytesAsPR(transport_descriptors_length);
                while (prDescriptors.hasBytes()) {
                    int descriptor_tag = prDescriptors.pull8();
                    int descriptor_length = prDescriptors.pull8();
                    PacketReader prDescriptor = prDescriptors.nextBytesAsPR(descriptor_length);
                    if (descriptor_tag == 0x44) { // cable_delivery_system_descriptor
                        ts.frequency = PacketReader.fromBCD(prDescriptor.pull32()) * 100;
                        ts.FEC_outer = prDescriptor.pull16() & 15; // 2 = RS(204/188) = Reed Solomon 204/188
                        switch(prDescriptor.pull8()) {
                            case 1: ts.modulation = C.fe_modulation.QAM_16; break;
                            case 2: ts.modulation = C.fe_modulation.QAM_32; break;
                            case 3: ts.modulation = C.fe_modulation.QAM_64; break;
                            case 4: ts.modulation = C.fe_modulation.QAM_128; break;
                            case 5: ts.modulation = C.fe_modulation.QAM_256; break;
                            default: ts.modulation = null; break;
                        }
                        long tmp = prDescriptor.pull32(); // BCD
                        switch ((int) (tmp & 15)) {
                            default: ts.FEC_inner = null; break;
                            case  1: ts.FEC_inner = C.dvbfe_code_rate.DVBFE_FEC_1_2; break;
                            case  2: ts.FEC_inner = C.dvbfe_code_rate.DVBFE_FEC_2_3; break;
                            case  3: ts.FEC_inner = C.dvbfe_code_rate.DVBFE_FEC_3_4; break;
                            case  4: ts.FEC_inner = C.dvbfe_code_rate.DVBFE_FEC_5_6; break;
                            case  5: ts.FEC_inner = C.dvbfe_code_rate.DVBFE_FEC_7_8; break;
                            case  6: ts.FEC_inner = C.dvbfe_code_rate.DVBFE_FEC_8_9; break;
                            case  7: ts.FEC_inner = null; break; // 3/5 ?
                            case  8: ts.FEC_inner = C.dvbfe_code_rate.DVBFE_FEC_4_5; break;
                            case  9: ts.FEC_inner = null; break; // 9/10 ?
                            case 15: ts.FEC_inner = C.dvbfe_code_rate.DVBFE_FEC_NONE; break;
                        }
                        ts.symbol_rate = PacketReader.fromBCD(tmp >> 4) * 100;
                    } else if (descriptor_tag == 0x41) { // service_list_descriptor
                        while (prDescriptor.hasBytes()) {
                            Service s = new Service();
                            s.service_id   = prDescriptor.pull16();
                            s.service_type = prDescriptor.pull8();
                            switch (s.service_type) {
                                case    1: s.service_type_txt = "TV"   ; break; // classic MPEG-2
                                case    2: s.service_type_txt = "Radio"; break;
                                case 0x0c: s.service_type_txt = "data" ; break; // data broadcast service
                                case 0x16: s.service_type_txt = "SD'"  ; break; // SDTV but with h.264
                                case 0x19: s.service_type_txt = "HD"   ; break; // HDTV with h.264
                                case 0x1f: s.service_type_txt = "UHD"  ; break; // HEVC (h.265?)
                                default: s.service_type_txt = String.format("0x%02x", s.service_type); break;
                            }
                            ts.services.add(s);
                        }
                    } else if (descriptor_tag == 0x83) { // Logical channel number descriptor
                        while (prDescriptor.hasBytes()) {
                            LogicalChannelNumber lcn = new LogicalChannelNumber();
                            lcn.service_id = prDescriptor.pull16();
                            int tmp = prDescriptor.pull16();
                            lcn.visible_service_flag = (tmp >> 15) > 0;
                            lcn.logical_channel_number = tmp & 1023;
                            ts.lcns.add(lcn);
                        }
                    } else {
//                        System.out.printf(" other_descriptor_tag=%x=", descriptor_tag);
//                        while (prDescriptor.hasBytes()) {
//                            System.out.printf("%02x", prDescriptor.pull8());
//                        }
                    }
                }
                ts.services.sort(Comparator.comparingInt(o -> o.service_id));
                serverWebSocketHandler.broadcast(ts);
            }
        }
    }

    public static class TransportStream {
        public String type = "transportStream";
        public int network_id;
        public int transport_stream_id;
        public int original_network_id;
        public long frequency;
        public int FEC_outer;
        public C.fe_modulation modulation;
        public long symbol_rate;
        public C.dvbfe_code_rate FEC_inner;
        public ArrayList<Service> services = new ArrayList<>();
        public ArrayList<LogicalChannelNumber> lcns = new ArrayList<>();
    }

    public static class Service {
        public int service_id;
        public int service_type;
        public String service_type_txt;
    }

    public static class LogicalChannelNumber {
        public int service_id;
        public boolean visible_service_flag;
        public int logical_channel_number;
    }

}
