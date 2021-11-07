package ch.oli.libdvbv5;

import ch.oli.ioctl.dvb_frontend_parameters;
import com.sun.jna.ptr.IntByReference;

public class LibDVBv5_Test {
    public static void main(String[] args) throws Exception {
//        System.out.println(new LibDVBv5.dvb_v5_fe_parms() );
        final LibDVBv5 I = LibDVBv5.INSTANCE;
        LibDVBv5.dvb_v5_fe_parms params = I.dvb_fe_open(1, 0, 0, 0);
        I.dvb_set_sys(params, LibDVBv5.fe_delivery_system.SYS_DVBC_ANNEX_A);
        I.dvb_fe_store_parm(params, LibDVBv5.DTV_FREQUENCY  , 338_000_000);
        I.dvb_fe_store_parm(params, LibDVBv5.DTV_INVERSION  , dvb_frontend_parameters.fe_modulation.QAM_256.ordinal());
        I.dvb_fe_store_parm(params, LibDVBv5.DTV_MODULATION , dvb_frontend_parameters.dvbfe_spectral_inversion.DVBFE_INVERSION_AUTO.ordinal());
        I.dvb_fe_store_parm(params, LibDVBv5.DTV_SYMBOL_RATE, 6_900_000  );
        I.dvb_fe_store_parm(params, LibDVBv5.DTV_INNER_FEC  , dvb_frontend_parameters.dvbfe_code_rate.DVBFE_FEC_NONE.ordinal());
        I.dvb_fe_set_parms(params);

        IntByReference value = new IntByReference();
        while(true) {
            I.dvb_fe_get_stats(params);
            I.dvb_fe_retrieve_stats(params,LibDVBv5.DTV_STAT_SIGNAL_STRENGTH, value);
            System.out.println(value.getValue());
            Thread.sleep(100);
        }
    }
}
