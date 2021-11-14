package ch.oli.ioctl;

import ch.oli.decode.PacketDecoderDVB;
import ch.oli.decode.PacketReader;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;

public class IoctlTest {

    private static Field FIELD_FD;
    private static int fdFrontend;

    private static final String ADAPTER = "/dev/dvb/adapter1";

    public static void main(String[] args) throws Exception {
        FIELD_FD = FileDescriptor.class.getDeclaredField("fd");
        FIELD_FD.setAccessible(true);

        RandomAccessFile fileFrontend = new RandomAccessFile(ADAPTER + "/frontend0", "rw");
        fdFrontend = (int) FIELD_FD.get(fileFrontend.getFD());

        dvb_frontend_info fei = new dvb_frontend_info();
        fei.getViaIoctl(fdFrontend);
        System.out.println("Frontend name: " + new String(fei.name));
        for (dvb_frontend_info.CAPS_BITS caps_bit : dvb_frontend_info.CAPS_BITS.values()) {
            if (((fei.caps >> caps_bit.ordinal()) & 1) > 0) {
                System.out.println(caps_bit.name());
            }
        }

//        for (int i = 0; i < 1000000; i++) {
//            tune(338_000_000); // "ZDF HD"
//        }
        for (int freq = 122_000_000; freq <= 870_000_000; freq += 8_000_000) {
            tune(freq);
        }
    }

    private static void tune(int freq) throws Exception {
        System.out.print("Tune to " + freq / 1000 / 1000.0 + " MHz : ");

        C.fe_delivery_system delSys = C.fe_delivery_system.SYS_DVBC_ANNEX_A;
        int                        symbolRate = 6_900_000;
        C.fe_modulation            modulation = C.fe_modulation.QAM_256;
        C.dvbfe_spectral_inversion inversion  = C.dvbfe_spectral_inversion.DVBFE_INVERSION_AUTO;
        C.dvbfe_code_rate          fec        = C.dvbfe_code_rate.DVBFE_FEC_NONE;
        if (freq >= 538_000_000 && freq <= 722_000_000) {
            symbolRate = 6_952_000;
        } else if (freq == 426_000_000) {
            modulation = C.fe_modulation.QAM_64;
        } else if (freq < 100_000_000) {
            // try to lock on DOCSIS-Upstream-Channels --> No luck
            symbolRate = 5_120_000;
            modulation = C.fe_modulation.QAM_64;
            fec = C.dvbfe_code_rate.DVBFE_FEC_AUTO;
        }

        long t0 = System.currentTimeMillis();

//        dtv_properties dtvprops = new dtv_properties();
//        dtvprops.addCmd(C.CMD.DTV_DELIVERY_SYSTEM, delSys    .ordinal());
//        dtvprops.addCmd(C.CMD.DTV_FREQUENCY      , freq                );
//        dtvprops.addCmd(C.CMD.DTV_SYMBOL_RATE    , symbolRate          );
//        dtvprops.addCmd(C.CMD.DTV_MODULATION     , modulation.ordinal());
//        dtvprops.addCmd(C.CMD.DTV_INVERSION      , inversion .ordinal());
//        dtvprops.addCmd(C.CMD.DTV_INNER_FEC      , fec       .ordinal());
//        dtvprops.addCmd(C.CMD.DTV_TUNE           , 0              );
//        dtvprops.setViaIoctl(fdFrontend);
//
//        dtvprops.clear();
//        dtvprops.addCmd(C.CMD.DTV_ENUM_DELSYS, new byte[0]);
//        dtvprops.getViaIoctrl(fdFrontend);


        dvb_frontend_parameters fep = new dvb_frontend_parameters();
        fep.frequency   = freq      ;
        fep.inversion   = inversion ;
        fep.symbol_rate = symbolRate;
        fep.fec_inner   = fec       ;
        fep.modulation  = modulation;
        fep.setViaIoctl(fdFrontend);

        dvb_frontend_status dfs = new dvb_frontend_status();
        while (true) {
//            dtvprops.clear();
//            dtvprops.addStatsCmd(C.CMD.DTV_STAT_SIGNAL_STRENGTH     );
//            dtvprops.addStatsCmd(C.CMD.DTV_STAT_CNR                 );
//            dtvprops.addStatsCmd(C.CMD.DTV_STAT_PRE_ERROR_BIT_COUNT );
//            dtvprops.addStatsCmd(C.CMD.DTV_STAT_PRE_TOTAL_BIT_COUNT );
//            dtvprops.addStatsCmd(C.CMD.DTV_STAT_POST_ERROR_BIT_COUNT);
//            dtvprops.addStatsCmd(C.CMD.DTV_STAT_POST_TOTAL_BIT_COUNT);
//            dtvprops.addStatsCmd(C.CMD.DTV_STAT_ERROR_BLOCK_COUNT   );
//            dtvprops.addStatsCmd(C.CMD.DTV_STAT_TOTAL_BLOCK_COUNT   );
//            dtvprops.getViaIoctrl(fdFrontend);
//            for (int i = 0; i < 8; i++) {
//                System.out.print(dtvprops.props[i].cmd + "=" + dtvprops.props[i].u.st.stat[0].value + " ");
//                switch(dtvprops.props[i].u.st.stat[0].scale) {
//                    case 0: System.out.print("n/a"); break;
//                    case 1: System.out.print("dBm"); break;
//                    case 2: System.out.print("rel"); break;
//                    case 3: System.out.print("cnt"); break;
//                    default: System.out.print("???"); break;
//                }
//                System.out.print("  ");
//            }
//            System.out.println();

            long millis = System.currentTimeMillis() - t0;
            dfs.getViaIoctl(fdFrontend);
//                System.out.println(dfs.status);
            if (dfs.status == 31) {
                System.out.println("LOCK in " + millis + " ms");
                break;
            }
            if (millis >= 500) {
                System.out.println("NO LOCK - giving up after " + millis + " ms");
                return;
            }
            Thread.sleep(10);
        }

        // we have a LOCK

        try (RandomAccessFile fileDemux = new RandomAccessFile(ADAPTER + "/demux0", "r")) {
            int fdDemux = (int) FIELD_FD.get(fileDemux.getFD());

            dmx_set_buffer_size dmxSb = new dmx_set_buffer_size();
            dmxSb.size = 65_536;
            dmxSb.setViaIoctl(fdDemux);

//            dmx_pes_filter_params dmxPes = new dmx_pes_filter_params();
//            dmxPes.pid = 0;
//            dmxPes.input = dmx_pes_filter_params.dmx_input.DMX_IN_FRONTEND;
//            dmxPes.output = dmx_pes_filter_params.dmx_output.DMX_OUT_TAP;
//            dmxPes.pes_type = dmx_pes_filter_params.dmx_ts_pes.DMX_PES_OTHER;
//            dmxPes.flags = dmx_sct_filter_params.DMX_CHECK_CRC | dmx_sct_filter_params.DMX_IMMEDIATE_START;
//            dmxPes.setViaIoctl(fdDemux);

            dmx_sct_filter_params dmxSct = new dmx_sct_filter_params();
            dmxSct.pid = 0;
            dmxSct.flags = dmx_sct_filter_params.DMX_CHECK_CRC | dmx_sct_filter_params.DMX_IMMEDIATE_START;
            dmxSct.timeout = 1000;
            dmxSct.setViaIoctl(fdDemux);

            byte[] buf = new byte[10_000];
//            FileOutputStream fos = new FileOutputStream("video.h264");
//            FileOutputStream fos = new FileOutputStream("audio.mp2a");


//            while (true) {
//                while (fileDemux.readByte() != 0) {
//                }
//                if (fileDemux.readByte() == 0) {
//                    if (fileDemux.readByte() == 0) {
//                        if (fileDemux.readByte() == 1) {
//                            fos.write(0);
//                            fos.write(0);
//                            fos.write(0);
//                            fos.write(1);
//                            break;
//                        }
//                    }
//                }
//            }
            for (int i = 0; i < 1; i++) {
                try {
                    int read = fileDemux.read(buf);
                    System.out.print("size=" + read + ": ");
//                    fos.write(buf,0,read);
                    PacketReader prPU = new PacketReader(buf, 0, read);
//                    DVB.hexDump(Arrays.copyOfRange(buf, 0, Math.min(read, 100)));
//                    System.out.println();
                    PacketDecoderDVB.decodePSI(prPU, dmxSct.pid);
                    System.out.println();
                } catch (IOException ex) {
                    // nothing received
                    ex.printStackTrace();
                }
            }
        }
    }

}
