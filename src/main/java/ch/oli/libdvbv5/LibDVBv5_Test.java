package ch.oli.libdvbv5;

import ch.oli.ioctl.C;
import com.sun.jna.ptr.IntByReference;

import java.io.FileInputStream;

public class LibDVBv5_Test {
    public static void main(String[] args) throws Exception {
        final LibDVBv5 I = LibDVBv5.INSTANCE;
        DVBdev dvb = new DVBdev(0,0, 0);
        dvb.setSys(C.fe_delivery_system.SYS_DVBC_ANNEX_A);
        dvb.tune(426_000_000, 6_900_000, C.fe_modulation.QAM_64, C.dvbfe_spectral_inversion.DVBFE_INVERSION_ON, C.dvbfe_code_rate.DVBFE_FEC_NONE);

        Thread.sleep(400);

        dvb.filter(0x2000);

        FileInputStream fis = new FileInputStream("/dev/dvb/adapter0/dvr0");
        IntByReference holder = new IntByReference();
        while (true) {
            I.dvb_fe_get_event(dvb.params);
            for (int cmd = 62; cmd < 70; cmd++) {
                I.dvb_fe_retrieve_stats(dvb.params, cmd, holder);
                System.out.print(cmd + "=" + holder.getValue() + "\t");
            }
            for (int cmd = 512; cmd < 517; cmd++) {
                I.dvb_fe_retrieve_stats(dvb.params, cmd, holder);
                System.out.print(cmd + "=" + holder.getValue() + "\t");
            }
//            for (int cmd = 1; cmd <= 62; cmd++) {
//                holder.setValue(-555);
//                I.dvb_fe_retrieve_parm(dvb.params, cmd, holder);
//                System.out.print(cmd + "=" + holder.getValue() + "\t");
//            }
            System.out.println();

            int avail = fis.available();
            if (avail > 0) {
                byte[] data = fis.readAllBytes();
                System.out.println(data.length);
            } else {
                Thread.sleep(100);
            }
        }
    }
}
