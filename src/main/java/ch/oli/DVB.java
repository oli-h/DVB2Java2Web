package ch.oli;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.BitSet;

public class DVB {

    private static final byte[] buf = new byte[188 * 10]; // room for N packets (each has 188 bytes)
    private static final BitSet pmtPIDs = new BitSet(8192); // we must learn which PIDs are to be decoded as PMT
    private static final MyBAOS[] payloadUnitBuffers = new MyBAOS[8192]; // buffer for each possible PID for reassembly
    private static final int[] lastContCounter = new int[8192]; // Continuity counter for each possoble PID
//    private static OutputStream fos;

    public static void main(String[] args) throws Exception {
//        fos = new FileOutputStream("video.h264");

        // use:  dvbv5-zap -rP "ZDF HD"
//        InputStream is = new FileInputStream("/dev/dvb/adapter0/dvr0");
        InputStream is = new FileInputStream("/home/oli/Desktop/2007_20211028072500.mpg");

        int count = 0;
        long t0 = System.nanoTime();
        while (true) {
            int read = is.read(buf);
            if (read < 0) {
                break;
            }
            count += read;
            long nanos = System.nanoTime() - t0;
            long bytesPreSecond = count * 1_000_000_000L / nanos;
//            System.out.println(read + ": " + bytesPreSecond * 8 / 1000 / 1000.0 + " MBit/s");
            int syncIndex = -1;
            for (int i = 0; i < 188; i++) {
                if (buf[i] == 0x47) {
                    syncIndex = i;
                    break;
                }
            }
            if (syncIndex < 0) {
                System.out.println("ERROR: no sync 0x47 found");
                continue;
            }

            for (int i = 0; syncIndex <= buf.length - 188; syncIndex += 188) {
//                hexDump(Arrays.copyOfRange(buf, syncIndex, syncIndex + 188));
//                System.out.println();
                PacketReader pr = new PacketReader(buf, syncIndex, 188);
                decodePacket(pr);
            }

//            for (int i = 0; i < 188; i++) {
//                System.out.format("%02X", buf[syncIndex + i] & 0xFF);
//                count++;
//                if (count % 88 == 0) {
//                    System.out.println();
//                    System.out.print(count+": ");
//                }
//            }
//            System.out.println();
        }
    }

    private static long prevPcrNanos = 0;
    private static long prevPcrValue = 0;


    public static void decodePacket(PacketReader pr) {
        // see https://en.wikipedia.org/wiki/MPEG_transport_stream#Packet
        if (pr.pull8() != 0x47) {
            System.out.println("ERROR: Sync 0x47 not found - ignoring packet");
            return; // silently ignore packets with wrong sync-byte
        }
        int tmp = pr.pull16();
        int pid = tmp & 0x1FFF;            // Packet Identifier, describing the payload data
        if (pid == 0x1FFF) {
            return; // silently ignore Null-Packets
        }
        boolean tei  = (tmp & 0x8000) > 0; // Transport error indicator
        if (pid == 0x1FFF || tei) {
            System.out.println("ERROR: Transport error indicator");
            return; // ignore packets with transport errors
        }
        boolean pusi = (tmp & 0x4000) > 0; // Payload unit start indicator
        boolean prio = (tmp & 0x2000) > 0; // Transport priority

        tmp = pr.pull8();
        int tsc = (tmp >> 6) & 3; // Transport scrambling control (0=FTA, 1=reserved, 2=even key 3=odd key)
        boolean hasAdaptField = (tmp & 0x20) > 0;
        boolean hasPayload = (tmp & 0x10) > 0;
        byte cc = (byte) (tmp & 15); // Continuity counter

        // Check "Continuity counter"
        int lastCC = lastContCounter[pid];
        int expectCC = (lastCC + (hasPayload ? 1 : 0)) & 15;
        if (lastCC > 0 && cc != expectCC) {
            System.out.format("ERROR: Continuity counter mismatch (expect %d but was %d) --> drop current reassembly for PID %d\n", expectCC, cc, pid);
            payloadUnitBuffers[pid] = null;
            return;
        }
        lastContCounter[pid] = 16 + cc; // +16 to move away from zero to show 'already seen'

        // fast filter
//        if (pid != 0) {
//            return;
//        }
//        if (pid != 16 && pid != 17 && pid != 0) {
//            return;
//        }
//        if (pid != 528) {
//            return;
//        }

        if (hasAdaptField) {
            int adaptFieldLen = pr.pull8();
            if (adaptFieldLen > 0) {
                PacketReader prAdapt = pr.nextBytesAsPR(adaptFieldLen);

                tmp = prAdapt.pull8();
                boolean discontinuityIndicator = (tmp & 0x80) > 0;
                boolean randomAccessIndicator  = (tmp & 0x40) > 0;
                boolean esPrioIndicator        = (tmp & 0x20) > 0;
                boolean pcrFlag                = (tmp & 0x10) > 0;
                boolean opcrFlag               = (tmp & 0x08) > 0;
                boolean splicingPointFlag      = (tmp & 0x04) > 0;
                boolean transportPrivateData   = (tmp & 0x02) > 0;
                boolean adaptFieldExtension    = (tmp & 0x01) > 0;
                if (pcrFlag) {
                    long pcr = prAdapt.pull32();
                    int ext = prAdapt.pull16();
                    pcr = (pcr << 1) | (ext >> 15);
                    ext &= 0x1FF;
                    pcr = pcr * 300 + ext;

                    long nanosNow = System.nanoTime();
                    long nanosDiff = nanosNow - prevPcrNanos;
                    prevPcrNanos = nanosNow;
                    long pcrDiff = pcr - prevPcrValue;
                    prevPcrValue = pcr;
                    long pcrPerSecond = pcrDiff * 1_000_000_000L / nanosDiff;
//                System.out.println(pcrPerSecond);
                }
            }
        }

        if (hasPayload) {
            MyBAOS payloadUnitBuffer = payloadUnitBuffers[pid];
            if (pusi && payloadUnitBuffer == null) {
                payloadUnitBuffer = new MyBAOS();
                payloadUnitBuffers[pid] = payloadUnitBuffer;
            } else if (pusi) {
                System.out.format("reassembled PU for PID%5d (size%6d):", pid, payloadUnitBuffer.size());
//                hexDump(payloadUnitBuffer.toByteArray());
//                System.out.println();
                PacketReader prPU = payloadUnitBuffer.asPacketReader();
                decodePayloadUnit(pid, prPU);
                payloadUnitBuffer.reset();
                System.out.println();
            }
            if (payloadUnitBuffer != null) {
                pr.writeRemainTo(payloadUnitBuffer);
            }
        }
    }

    private static void hexDump(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            System.out.format("%02X", data[i] & 0xFF);
        }
    }

    private static void decodePayloadUnit(int pid, PacketReader prPU) {
//        boolean isPSI = (pid == 0 || pid == 16 || pid == 17);
        boolean isPSI = (pid < 32); // simplified "all lower PIDs" to not handle each potential PID individually
        if (isPSI || pmtPIDs.get(pid)) {
            decodePSI(prPU, pid);
            return;
        }
        decodePES(prPU);
    }

    /**
     * PSI = "Program Specific Information"
     * Transferred in PID 0 (PAT), PID 16 (NIT, ST), PID 17 (SDT, BAT, ST), PID 18 (EIT, ST, CIT)
     * and some others
     */
    private static void decodePSI(PacketReader prPU, int pid) {
        // https://www.etsi.org/deliver/etsi_en/300400_300499/300468/01.16.01_60/en_300468v011601p.pdf
        int payloadPointer = prPU.pull8();
        prPU.skip(payloadPointer);

//            hexDump(prPU.wholePacket());
//            System.out.println();

        while (prPU.hasBytes()) {
            int table_id = prPU.pull8();
            if (table_id == 0xFF) {
                break;
            }
            int tmp = prPU.pull16();
            int section_length = tmp & 0xFFF; // 12 Bit
            int maxSectLen = 1021; // // lots of section have a max-size of 1021 ...
            if (pid == 0x12 || pid == 0x15) { // ... but EIT-Tables have 4093 (and "network synchronization probably also")
                maxSectLen = 4093;
            }
            if (section_length > maxSectLen) {
                System.out.format("ERROR: sectLen %d larger than allowed %d. Ignoring rest of packet unit", section_length, maxSectLen);
                break;
            }
            PacketReader prSection = prPU.nextBytesAsPR(section_length);
            int something = prSection.pull16(); // semantic depends on table_id - still we need to decode it here
            tmp = prSection.pull8();
            int version_number = (tmp >> 1) & 0x1F;
            int current_next_indicator = tmp & 1; // 1=current 0=next
            int section_number = prSection.pull8();
            int last_section_number = prSection.pull8();

//                System.out.format("(TableID %d sectLen=%4d Section %d/%d)", table_id, section_length, section_number, last_section_number);

            // in PID 0: Program Association Table (PAT)
            if (table_id == 0x00) {
                decodePAT(prSection, table_id ,something);
            }

            // in PID 16: Network Information Table (NIT)
            else if (table_id == 0x40 || table_id == 0x41) { // actual_network (0x40) or other_network 0x41
                decodeNIT(prSection, table_id, something);
            }

            // in PID 17: Service Description Table (SDT)
            else if (table_id == 0x42 || table_id == 0x46) { // actual_transport_stream (0x42) or other_transport_stream (0x46)
                decodeSDT(prSection, table_id, something);
            }

            // in some PID: Program Map Table (PMT)
            else if (table_id == 0x02) { // program_map_section
                decodePMT(prSection, table_id, something);
            }

        }
    }

    private static void decodePAT(PacketReader prSection, int table_id, int something) {
        System.out.printf(" PAT:");
        while (prSection.remain() > 4) {
            int program = prSection.pull16();
            int pmtPID = prSection.pull16() & 0x1FFF;
            System.out.format(" Prog:%d->PMT-PID:%d", program, pmtPID);
            pmtPIDs.set(pmtPID);
        }

//        int crc32 = 0xffffffff;
//        // we must include the 3 bytes containing "table_id" and "section_length"
//        byte[] tmp = Arrays.copyOfRange(prSection.buf, prSection.initialOffset - 3, prSection.initialOffset + prSection.initialLength);
//        for (int i = 0; i < tmp.length; i++)
//        {
//            byte b = tmp[i];
//            for (int bit = 0; bit < 8; bit++)
//            {
//                if ((crc32 < 0) != (b < 0))
//                    crc32 = (crc32 << 1) ^ 0x04C11DB7;
//                else
//                    crc32 = (crc32 << 1);
//                b <<= 1;
//            }
//        }
//        // crc32 must be zero
//        System.out.printf(" ----------- %08x ----------- ", crc32);
    }

    private static void decodeNIT(PacketReader prSection, int table_id, int something) {
        int network_id = something;
//        if (network_id != 43020) {
//            return;
//        }

        System.out.printf(" NIT: %s network_id=%d", table_id == 0x40 ? "actual" : "other ", network_id);

        {
            int network_descriptors_length = prSection.pull16() & 0xFFF;
            PacketReader prDescriptors = prSection.nextBytesAsPR(network_descriptors_length);
            while (prDescriptors.hasBytes()) {
                int descriptor_tag = prDescriptors.pull8();
                int descriptor_length = prDescriptors.pull8();
                PacketReader prDescriptor = prDescriptors.nextBytesAsPR(descriptor_length);
                if (descriptor_tag == 0x40) { // 0x40=network_name_descriptor
                    String network_name = prDescriptor.pullChar(descriptor_length);
                    System.out.format(" network_name='%s'", network_name);
                } else if (descriptor_tag == 0x4a) { // linkage_descriptor
                } else if (descriptor_tag == 0x5f) { // private_data_specifier_descriptor
                } else if (descriptor_tag == 0x84) { // ???
                } else if (descriptor_tag == 0x87) { // ???
                } else if (descriptor_tag == 0x89) { // ???
                } else if (descriptor_tag == 0xd2) { // ???
                } else {
                    System.out.printf(" other descriptor_tag=%x",descriptor_tag);
                }
                // also seen (decimal): 74
            }
        }

        {
            int transport_stream_loop_length = prSection.pull16() & 0xFFF;
            PacketReader prTransportStreams = prSection.nextBytesAsPR(transport_stream_loop_length);
            while (prTransportStreams.hasBytes()) {
                int transport_stream_id = prTransportStreams.pull16();
                int original_network_id = prTransportStreams.pull16();
                System.out.printf(" tsid=%03d", transport_stream_id);
                int transport_descriptors_length = prTransportStreams.pull16() & 0xFFF;
                PacketReader prDescriptors = prTransportStreams.nextBytesAsPR(transport_descriptors_length);
                while (prDescriptors.hasBytes()) {
                    int descriptor_tag = prDescriptors.pull8();
                    int descriptor_length = prDescriptors.pull8();
                    PacketReader prDescriptor = prDescriptors.nextBytesAsPR(descriptor_length);
                    if (descriptor_tag == 0x44) { // cable_delivery_system_descriptor
                        long frequency = prDescriptor.pull32();  // BCD
                        int FEC_outer  = prDescriptor.pull16() & 15;
                        int modulation = prDescriptor.pull8();
                        long symbol_rate = prDescriptor.pull32();
                        int FEC_inner = (int) (symbol_rate & 15);
                        symbol_rate >>= 4;
                        System.out.printf(" freq=%x mod=%d rate=%x", frequency >> 16, modulation, symbol_rate >> 4);
                    } else if (descriptor_tag == 0x41) { // service_list_descriptor
                        while(prDescriptor.hasBytes()) {
                            int service_id   = prDescriptor.pull16();
                            int service_type = prDescriptor.pull8();
                            switch(service_type) {
                                case 0x01: System.out.print(" TV="); break;
                                case 0x02: System.out.print(" Radio="); break;
                                case 0x19: System.out.print(" HDTV="); break;
                                default: System.out.format(" 0x%x=", service_id); break;
                            }
                            System.out.print(service_id);
                        }
                    } else {
//                        System.out.printf(" other descriptor_tag=%x",descriptor_tag);
                    }
                }
            }
        }
    }

    private static void decodeSDT(PacketReader prSection, int table_id, int transport_stream_id) {
        System.out.printf(" SDT:");
        int original_network_id = prSection.pull16();
        int reserved_future_use = prSection.pull8();
        while (prSection.remain() > 4) {
            int service_id = prSection.pull16();
            int tmp = prSection.pull8();
            boolean EIT_schedule_flag = (tmp & 2) > 0;
            boolean EIT_present_following_flag = (tmp & 1) > 0;
            tmp = prSection.pull16();
            int running_status = (tmp >> 13) & 7; // 0=undefined 1=not running 2=starts in a few seconds (e.g. for video recording) 3=pausing 4=running
            boolean free_CA_mode = (tmp & 0x1000) > 0; // false=FTA, true=scrambled

            System.out.printf(" %d/%s/%s", service_id, transport_stream_id, free_CA_mode?"CA":"FTA");

            int descriptors_loop_length = tmp & 0xFFF;
            PacketReader prDescriptors = prSection.nextBytesAsPR(descriptors_loop_length);
            while (prDescriptors.hasBytes()) {
                int descriptor_tag = prDescriptors.pull8();
                int descriptor_length = prDescriptors.pull8();
                PacketReader prDescriptor = prDescriptors.nextBytesAsPR(descriptor_length);
                if (descriptor_tag == 0x48) { // 0x48=service_descriptor
                    int service_type = prDescriptor.pull8();
                    int service_provider_name_length = prDescriptor.pull8();
                    String service_provider_name = prDescriptor.pullChar(service_provider_name_length); // z.B. "upc"
                    int service_name_length = prDescriptor.pull8();
                    String service_name = prDescriptor.pullChar(service_name_length);
                    System.out.printf("/%s", service_name);
                }
            }
//            System.out.println();
        }
        if (prSection.remain() != 4) {
            System.out.printf("Not exactly 32 Bit (CRC) remaining");
        }
    }

    private static void decodePMT(PacketReader prSection, int table_id, int something) {
        int program_number = something;
        int pcr_pid = prSection.pull16() & 0x1FFF;
        System.out.format(" PMT program_number=%d pcr_pid=%d", program_number, pcr_pid);

        {
            int program_info_length = prSection.pull16() & 0xFFF;
            PacketReader prProgramInfo = prSection.nextBytesAsPR(program_info_length);
        }
        while (prSection.remain() > 4) {
            int stream_type = prSection.pull8();
            int elementary_PID = prSection.pull16() & 0x1FFF;
            int ES_info_length = prSection.pull16() & 0xFFF;
            prSection.nextBytesAsPR(ES_info_length);
            System.out.format(" %02x->%d", stream_type, elementary_PID);
        }
    }

    /**
     * PES = Packetized Elementary Stream
     * here is where we find video- and audio-streams
     */
    private static void decodePES(PacketReader prPU) {
        // PES - see http://dvd.sourceforge.net/dvdinfo/pes-hdr.html
        int prefixHi = prPU.pull16();
        int prefixLo = prPU.pull8();
        if (prefixHi != 0 || prefixLo != 1) {
            System.out.print("ERROR: PES not correct prefix - expect 0x000001");
            return;
        }
        int streamId       = prPU.pull8(); // Audio streams (0xC0-0xDF), Video streams (0xE0-0xEF)
        int pesLength      = prPU.pull16(); // 0 allowed for video streams
        if (streamId >= 0xC0) { // Audio & Video-Stream have "extension present"
            int tmp = prPU.pull8();
            int PES_scrambling_control   = (tmp >> 4) & 3; // 0=FTA, 1=reserved, 2=even key 3=odd key
            int PES_priority             = (tmp >> 3) & 1;
            int data_alignment_indicator = (tmp >> 2) & 1; // 1=indicates that PES packet header is immediately followed by video start code or audio syncword.
            int copyright                = (tmp >> 1) & 1;
            int original_or_copy         = (tmp     ) & 1; // 1 = original, 0 = copy
            tmp = prPU.pull8();
            int PTS_DTS_flags             = (tmp >> 6) & 3; // 0=no PTS, 1=forbidden, 2=PTS, 3=PTS and DTS
            int ESCR_flag                 = (tmp >> 5) & 1; // 1=Elementary Stream Clock Reference present
            int ES_rate_flag              = (tmp >> 4) & 1; // 1=ES-rate present
            int DSM_trick_mode_flag       = (tmp >> 3) & 1;
            int additional_copy_info_flag = (tmp >> 2) & 1;
            int PES_CRC_flag              = (tmp >> 1) & 1;
            int PES_extension_flag        = (tmp     ) & 1;
            // ...
            int PES_header_data_length = prPU.pull8();
            System.out.format(" PES-Packet Stream-ID %02X len=%4d", streamId, pesLength);

            {
                PacketReader prHeader = prPU.nextBytesAsPR(PES_header_data_length);
                if (PTS_DTS_flags >= 2) {
                    long pts = prHeader.pull8() & 7;
                    pts = (pts << 15) | (prHeader.pull16() & 0x7FFF);
                    pts = (pts << 15) | (prHeader.pull16() & 0x7FFF);
                    System.out.format(" PTS %.2f", pts / 90000.);
                }
                if (PTS_DTS_flags >= 3) {
                    long dts = prHeader.pull8() & 7;
                    dts = (dts << 15) | (prHeader.pull16() & 0x7FFF);
                    dts = (dts << 15) | (prHeader.pull16() & 0x7FFF);
//                        System.out.format(" DTS %.2f", dts / 90000.);
                }
                if (ESCR_flag == 1) {
                    long escr = prHeader.pull16();
                    escr = (escr << 16) | prHeader.pull16();
                    escr = (escr << 16) | prHeader.pull16(); // TODO: need to shift the bits a bit
//                        System.out.format(" ESCR %d", escr);
                }
                if (ES_rate_flag == 1) {
                    int es_rate = prHeader.pull8() & 0x7F;
                    es_rate = (es_rate << 16) | (prHeader.pull16());
                    es_rate >>= 1;
//                        System.out.format(" ES-rate %d bytes/sec", es_rate * 50);
                }
            }
//                prPU.writeRemainTo(fos);
//                long start_code = prPU.pull32();
//                System.out.format(" start_code=%08x", start_code);
        }
    }

}
