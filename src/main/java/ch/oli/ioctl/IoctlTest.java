package ch.oli.ioctl;

public class IoctlTest {

    public static void main(String[] args) throws Exception {
        int fdFrontend = LibC.x.open("/dev/dvb/adapter0/frontend0", LibC.O_RDWR);
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

        dvb_frontend_parameters fep = new dvb_frontend_parameters();
        dvb_frontend_status dfs = new dvb_frontend_status();
        for (int freq = 122_000_000; freq <= 862_000_000; freq += 8_000_000) {

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

            while (true) {
                Thread.sleep(10);
                long millis = System.currentTimeMillis() - t0;
                dfs.getViaIoctl(fdFrontend);
//                System.out.println(dfs.status);
                if (dfs.status == 31) {
                    System.out.println("LOCK in " + millis + " ms");
                    break;
                }
                if (millis >= 200) {
                    System.out.println("NO LOCK - giving up after " + millis + " ms");
                    break;
                }
            }
        }
    }

}
