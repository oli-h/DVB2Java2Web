package ch.oli.web;

import ch.oli.decode.PacketReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.JulianFields;

@Service
public class DecoderEIT {

    @Autowired
    private ServerWebSocketHandler serverWebSocketHandler;

    public void decode(PacketReader prSection, int something) {
        int service_id = something;

        int transport_stream_id = prSection.pull16();
        int original_network_id = prSection.pull16();
        int segment_last_section_number = prSection.pull8();
        int last_table_id = prSection.pull8();

        while (prSection.remain() > 4) {
            EventInformation ei = new EventInformation();
            ei.service_id = service_id;
            ei.event_id = prSection.pull16();

            int start_time_mjd   = prSection.pull16();
            LocalDate start_date = LocalDate.MIN.with(JulianFields.MODIFIED_JULIAN_DAY, start_time_mjd);
            int hh = PacketReader.fromBCD(prSection.pull8());
            int mm = PacketReader.fromBCD(prSection.pull8());
            int ss = PacketReader.fromBCD(prSection.pull8());
            LocalTime start_hhmmss = LocalTime.of(hh, mm, ss);
            ei.start_time = LocalDateTime.of(start_date, start_hhmmss).toEpochSecond(ZoneOffset.UTC);

            hh = PacketReader.fromBCD(prSection.pull8());
            mm = PacketReader.fromBCD(prSection.pull8());
            ss = PacketReader.fromBCD(prSection.pull8());
            ei.duration = hh * 3600 + mm * 60 + ss;

            int tmp = prSection.pull16();
            ei.running_status   = (tmp >> 13) & 7;
            ei.free_CA_mode = (tmp & 0x1000) > 0;

            int descriptors_loop_length = tmp & 0xFFF;
            PacketReader prDescriptors = prSection.nextBytesAsPR(descriptors_loop_length);
            while (prDescriptors.hasBytes()) {
                int descriptor_tag = prDescriptors.pull8();
                int descriptor_length = prDescriptors.pull8();
                PacketReader prDescriptor = prDescriptors.nextBytesAsPR(descriptor_length);
                if (descriptor_tag == 0x4d) { // 0x4d=short_event_descriptor
                    String ISO_639_language_code = prDescriptor.pullChar(3);
                    int event_name_length = prDescriptor.pull8();
                    if (event_name_length > 0) {
                        ei.event_name = prDescriptor.pullChar(event_name_length);
                    }
                    int text_length = prDescriptor.pull8();
                    if (text_length > 0) {
                        ei.text = prDescriptor.pullChar(text_length);
                    }
                } else if (descriptor_tag == 0x4e) { // 0x4e=extended_event_descriptor
                } else if (descriptor_tag == 0x50) { // 0x50=component_descriptor
                } else if (descriptor_tag == 0x54) { // 0x54=content_descriptor
                } else if (descriptor_tag == 0x55) { // 0x55=parental_rating_descriptor
                } else if (descriptor_tag == 0x76) { // 0x76=content_identifier_descriptor
                } else {
                }
            }
            serverWebSocketHandler.broadcast(ei);
        }

        // 4 bytes remain CRC - ignored
    }

    public static class EventInformation {
        public String type = "eit";
        public int service_id;
        public int event_id;
        public long start_time; // seconds since 1.1.1970
        public long duration;   // seconds
        public int running_status;
        public boolean free_CA_mode;
        public String event_name;
        public String text;
    }

}
