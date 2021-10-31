package ch.oli.ioctl;

import ch.oli.DVB;
import ch.oli.PacketReader;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.util.Arrays;

public class IoctlTest {

    private static Field FIELD_FD;
    private static int fdFrontend;

    public static void main(String[] args) throws Exception {
        FIELD_FD = FileDescriptor.class.getDeclaredField("fd");
        FIELD_FD.setAccessible(true);

        RandomAccessFile fileFrontend = new RandomAccessFile("/dev/dvb/adapter0/frontend0","rw");
        fdFrontend = (int) FIELD_FD.get(fileFrontend.getFD());

//        errno();
//        System.out.println(2158522173L);
//        System.out.println(LibC.FE_GET_INFO);
        dvb_frontend_info fei = new dvb_frontend_info();
        fei.getViaIoctl(fdFrontend);
//        for (dvb_frontend_info.CAPS_BITS caps_bit : dvb_frontend_info.CAPS_BITS.values()) {
//            if ((fei.caps >> caps_bit.ordinal()) > 0) {
//                System.out.println(caps_bit.name());
//            }
//        }

//        for (int freq = 122_000_000; freq <= 862_000_000; freq += 8_000_000) {
//            tune(freq);
//        }
        while (true) {
            tune(338_000_000); // ZDF HD etc.
        }
    }

    private static void tune(int freq) throws Exception {
        dvb_frontend_parameters fep = new dvb_frontend_parameters();
        fep.frequency   = freq;
        fep.inversion   = dvb_frontend_parameters.dvbfe_spectral_inversion.DVBFE_INVERSION_AUTO;
        fep.symbol_rate = 6_900_000;
        fep.fec_inner   = dvb_frontend_parameters.dvbfe_code_rate.DVBFE_FEC_NONE;
        fep.modulation  = dvb_frontend_parameters.fe_modulation.QAM_256;
        if (freq == 426_000_000) {
            fep.modulation = dvb_frontend_parameters.fe_modulation.QAM_64;
        }
        if (freq >= 538_000_000 && freq <= 722_000_000) {
            fep.symbol_rate = 6_952_000;
        }
        System.out.print("Tune to " + freq / 1000 / 1000.0 + " MHz : ");
        long t0 = System.currentTimeMillis();
        fep.setViaIoctl(fdFrontend);

        dvb_frontend_status dfs = new dvb_frontend_status();
        while (true) {
            long millis = System.currentTimeMillis() - t0;
            dfs.getViaIoctl(fdFrontend);
//                System.out.println(dfs.status);
            if (dfs.status == 31) {
                System.out.println("LOCK in " + millis + " ms");
                break;
            }
            if (millis >= 200) {
                System.out.println("NO LOCK - giving up after " + millis + " ms");
                return;
            }
            Thread.sleep(10);
        }

        // we have a LOCK

        try (RandomAccessFile fileDemux = new RandomAccessFile("/dev/dvb/adapter0/demux0", "r")) {
            int fdDemux = (int) FIELD_FD.get(fileDemux.getFD());

            dmx_set_buffer_size dmxSb = new dmx_set_buffer_size();
            dmxSb.size = 65_536;
            dmxSb.setViaIoctl(fdDemux);

            dmx_pes_filter_params dmxPes = new dmx_pes_filter_params();
            dmxPes.pid = 272;
            dmxPes.input = dmx_pes_filter_params.dmx_input.DMX_IN_FRONTEND;
            dmxPes.output = dmx_pes_filter_params.dmx_output.DMX_OUT_TAP;
            dmxPes.pes_type = dmx_pes_filter_params.dmx_ts_pes.DMX_PES_OTHER;
            dmxPes.flags = 5;
            dmxPes.setViaIoctl(fdDemux);

//            dmx_sct_filter_params dmxSct = new dmx_sct_filter_params();
//            dmxSct.pid = 292;
//            dmxSct.flags = 5;
//            dmxSct.timeout = 1000;
//            dmxSct.setViaIoctl(fdDemux);

            byte[] buf = new byte[10_000];
            FileOutputStream fos = new FileOutputStream("video.h264");
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
            for (int i = 0; i < 1_000_000; i++) {
                try {
                    int read = fileDemux.read(buf);
                    System.out.print(read + ": ");
                    fos.write(buf,0,read);
                    PacketReader prPU = new PacketReader(buf, 0, read);
//                    DVB.hexDump(Arrays.copyOfRange(buf, 0, Math.min(read, 100)));
                    System.out.println();
//                    DVB.decodePSI(prPU, dmxSct.pid);
//                    System.out.println();
                } catch (IOException ex) {
                    // nothing received
                    ex.printStackTrace();
                }
            }
        }
    }

}
