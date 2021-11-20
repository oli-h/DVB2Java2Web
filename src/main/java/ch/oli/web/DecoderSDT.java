package ch.oli.web;

import ch.oli.decode.PacketReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class DecoderSDT {

    @Autowired
    private ServerWebSocketHandler serverWebSocketHandler;

    public void decode(PacketReader prSection, int table_id, int something) {
        int transport_stream_id = something;

        int original_network_id = prSection.pull16();
        int reserved_future_use = prSection.pull8();

        ServiceDescriptors sds = new ServiceDescriptors();
        while (prSection.remain() > 4) {
            ServiceDescriptor sd = new ServiceDescriptor();
            sds.serviceDescriptors.add(sd);
            sd.service_id = prSection.pull16();
            int tmp = prSection.pull8();
            sd.EIT_schedule_flag = (tmp & 2) > 0;
            sd.EIT_present_following_flag = (tmp & 1) > 0;
            tmp = prSection.pull16();
            sd.running_status = (tmp >> 13) & 7;
            sd.free_CA_mode = (tmp & 0x1000) > 0;

            int descriptors_loop_length = tmp & 0xFFF;
            PacketReader prDescriptors = prSection.nextBytesAsPR(descriptors_loop_length);
            while (prDescriptors.hasBytes()) {
                int descriptor_tag = prDescriptors.pull8();
                int descriptor_length = prDescriptors.pull8();
                PacketReader prDescriptor = prDescriptors.nextBytesAsPR(descriptor_length);
                if (descriptor_tag == 0x48) { // 0x48=service_descriptor
                    sd.service_type = prDescriptor.pull8();
                    int service_provider_name_length = prDescriptor.pull8();
                    sd.service_provider_name = prDescriptor.pullChar(service_provider_name_length);
                    int service_name_length = prDescriptor.pull8();
                    sd.service_name = prDescriptor.pullChar(service_name_length);
                }
            }
        }
        serverWebSocketHandler.broadcast(sds);
//        if (prSection.remain() != 4) {
//            System.out.printf("Not exactly 32 Bit (CRC) remaining");
//        }
    }

    public static class ServiceDescriptors {
        public String type = "serviceDescriptors";
        public ArrayList<ServiceDescriptor> serviceDescriptors = new ArrayList<>();
    }

    public static class ServiceDescriptor {
        public int service_id;
        public boolean EIT_schedule_flag;
        public boolean EIT_present_following_flag;
        public int running_status;  // 0=undefined 1=not running 2=starts in a few seconds (e.g. for video recording) 3=pausing 4=running
        public boolean free_CA_mode; // false=FTA, true=scrambled

        // from Descriptor 0x48
        public int service_type;
        public String service_provider_name;  // z.B. "upc"
        public String service_name;
    }
}
