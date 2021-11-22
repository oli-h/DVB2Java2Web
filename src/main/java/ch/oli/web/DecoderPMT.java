package ch.oli.web;

import ch.oli.decode.PacketReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;

@Service
public class DecoderPMT {

    @Autowired
    private ServerWebSocketHandler serverWebSocketHandler;

    // see https://ocw.unican.es/pluginfile.php/171/course/section/78/iso13818-1.pdf

    public void decode(PacketReader prSection, int something) {
        ProgramMap pm = new ProgramMap();
        pm.service_id = something;
        pm.pcr_pid    = prSection.pull16() & 0x1FFF;

        {
            int program_info_length = prSection.pull16() & 0xFFF;
            PacketReader prProgramInfo = prSection.nextBytesAsPR(program_info_length); // here we find CA_descriptors
        }
        while (prSection.remain() > 4) {
            StreamInfo si = new StreamInfo();
            pm.siList.add(si);
            si.stream_type = prSection.pull8();
            switch (si.stream_type) {
                case   2: si.stream_type_txt = "ITU-T Rec. H.262 | ISO/IEC 13818-2 Video | ISO/IEC 11172-2 constr. parameter video stream"; break;
                case   3: si.stream_type_txt = "ISO/IEC 11172 Audio"; break;
                case   4: si.stream_type_txt = "ISO/IEC 13818-3 Audio"; break;
                case   5: si.stream_type_txt = "ITU-T Rec. H.222.0 | ISO/IEC 13818-1 private sections"; break;
                case   6: si.stream_type_txt = "ITU-T Rec. H.222.0 | ISO/IEC 13818-1 PES packets containing private data"; break;
                case  11: si.stream_type_txt = "ISO/IEC 13818-6 DSM-CC U-N Messages"; break;
                case  12: si.stream_type_txt = "ISO/IEC 13818-6 Stream Descriptors"; break;
                case  15: si.stream_type_txt = "ISO/IEC 13818-7 Audio with ADTS transport sytax"; break; // AAC Audio
                case  27: si.stream_type_txt = "AVC video stream as defined in ITU-T Rec. H.264 | ISO/IEC 14496-10 Video"; break;
                case  36: si.stream_type_txt = "ITU-T Rec. H.222.0 | ISO/IEC 13818-1 reserved"; break; // UHD Video
                case 128: si.stream_type_txt = "User private"; break; // e.g. software updates
                default:
                    System.out.println("Unknown stream_type " + si.stream_type + " for program_number " + pm.service_id);
                    break;
            }
            si.elementary_PID = prSection.pull16() & 0x1FFF;
            int ES_info_length = prSection.pull16() & 0xFFF;
            PacketReader prDescriptors = prSection.nextBytesAsPR(ES_info_length);
            while (prDescriptors.hasBytes()) {
                int descriptor_tag = prDescriptors.pull8();
                int descriptor_length = prDescriptors.pull8();
                PacketReader prDescriptor = prDescriptors.nextBytesAsPR(descriptor_length);
                if (descriptor_tag == 0x52) { // 0x52=stream_identifier_descriptor
                    si.stream_identifier = prDescriptor.pull8();
                } else if (descriptor_tag == 0x02) { // 0x02=video_stream_descriptor
                    // e.g. Frame-Rate, MPEG-2 Profile etc.
                } else if (descriptor_tag == 0x03) { // 0x03=audio_stream_descriptor
                    // e.g. layer, variable_rate_audio_indicator
                } else if (descriptor_tag == 0x05) { // 0x05=registration_descriptor
                    // e.g. contains hints about audio format, e.g. for "AC-3"
                } else if (descriptor_tag == 0x06) { // 0x06=data_stream_alignment_descriptor
                    // gives hints to align audio with video ??
                } else if (descriptor_tag == 0x0a) { // 0x0a=ISO_639_language_descriptor
                    si.ISO_639_language_code = prDescriptor.pullChar(3);
                    si.audio_type = prDescriptor.pull8();
                } else if (descriptor_tag == 0x0e) { // 0x0e=maximum_bitrate_descriptor
//                    si.maximum_bitrate = prDescriptor.pull24() * 50;
                } else if (descriptor_tag == 0x28) { // 0x28=AVC_video_descriptor
                    // ???
                } else if (descriptor_tag == 0x2a) { // 0x22=AVC_timing_and_HRD_descriptor
                    // ???
                } else if (descriptor_tag == 0x38) { // 0x38=ITU-T.Rec.H.222.0|ISO/IEC13818-1 Reserved
                    // ???
                } else if (descriptor_tag == 0x45) { // 0x45=VBI_data_descriptor
                    // e.g.
                } else if (descriptor_tag == 0x56) { // 0x56=teletext_descriptor
                    while (prDescriptor.hasBytes()) {
                        String ISO_639_language_code = prDescriptor.pullChar(3);
                        int tmp = prDescriptor.pull8();
                        int teletext_type = (tmp >> 3); // 0=reserved 1=initial page 2=subtitle page 3=additional information page 4=programme schedule page 5=subtitle page for hearing impaired people
                        int teletext_magazine_number = tmp & 7; // 1=100..199, 2=200..299, ... , 0=800..899
                        if (teletext_magazine_number == 0) {
                            teletext_magazine_number = 8;
                        }
                        int teletext_page_number = prDescriptor.pull8();
                        if (teletext_type == 1) {
                            si.teletextInitialPage = teletext_magazine_number * 100 + teletext_page_number;
                        }
                    }
                } else if (descriptor_tag == 0x59) { // 0x59=subtitling_descriptor
                } else if (descriptor_tag == 0x6a) { // 0x6a=AC3_descriptor
                    si.ac3Audio = true;
                } else if (descriptor_tag == 0x6b) { // 0x6b=ancillary_data_descriptor
                    // for audio streams. Contains hints for e.g "RDS via UECP" or other embedded data
                } else if (descriptor_tag == 0x6f) { // 0x6f=application_signalling_descriptor
                    si.hbbtv = true;
                } else if (descriptor_tag == 0x7f) { // 0x7f=extension_descriptor
                    // e.g. image icon descriptor
                    // e.g. Content Protection/Copy Management (CPCM) delivery signalling descriptor.
                    // e.g. DVB-T2 or DVB-SH delivery system descriptor.
                    // e.g. supplementary audio descriptor
                    // e.g. network change notify descriptor
                    // e.g. message descriptor
                    // ...
                } else if (descriptor_tag == 0x0f) { // 0x0f=private_data_indicator_descriptor
                    // ??? (we see String "UPC" in the 32-bit-word)
                } else if (descriptor_tag == 0x5f) { // 0x5f=private_data_specifier_descriptor
                    // ???
                } else if (descriptor_tag == 0x66) { // 0x66=data_broadcast_id_descriptor
                    // ???
                } else if (descriptor_tag == 0x7c) { // 0x7c=AAC_descriptor
                    //
                } else if (descriptor_tag == 0x80) { // 0x80=User defined
                    // ???
                } else if (descriptor_tag == 0x81) { // 0x81=User defined
                    // ???
                } else if (descriptor_tag == 0x90) { // 0x90=User defined/ATSC reserved
                    // ???
                } else if (descriptor_tag == 0xc3) { // 0xc3=User defined
                    // ???
                } else if (descriptor_tag == 0xc5) { // 0xc5=User defined
                    // ???
                } else if (descriptor_tag == 0xfd) { // 0xfd=User defined
                    // ???
                } else if (descriptor_tag == 0xfe) { // 0xfe=User defined
                    // ???
                } else {
                    System.out.format("Unknown descriptor_tag 0x%02X for program_number %d\n", descriptor_tag, pm.service_id);
                }

            }
        }
        serverWebSocketHandler.broadcast(pm);

        // 4 bytes remain CRC - ignored
    }

    public static class ProgramMap {
        public String type = "pmt";
        public int service_id; // = program_number
        public int pcr_pid;
        public ArrayList<StreamInfo> siList = new ArrayList<>();
    }

    public static class StreamInfo {
        public int stream_type       ;
        public String stream_type_txt;
        public int elementary_PID    ;
        public int stream_identifier ;
//        public int maximum_bitrate  ; // bytes/s
        public String ISO_639_language_code;
        public int    audio_type           ; // 0=Undefined 1=Clean effects 2=Hearing impaired 3=Visual impaired commentary
        public boolean ac3Audio            ; // true = when descriptor 0x6a present
        public int teletextInitialPage     ; // !=0 when stream is Teletext
        public boolean hbbtv;
    }

}
