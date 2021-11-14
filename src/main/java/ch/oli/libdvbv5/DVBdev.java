package ch.oli.libdvbv5;

import ch.oli.ioctl.dmx_pes_filter_params;
import ch.oli.ioctl.dmx_sct_filter_params;
import ch.oli.ioctl.dvb_frontend_parameters;

public class DVBdev {

    private static final LibDVBv5 I = LibDVBv5.INSTANCE;
    public final LibDVBv5.dvb_v5_fe_parms params;
    private final int dmxfd;

    public DVBdev(int adapter, int frontend, int demux) {
        params = I.dvb_fe_open(adapter, frontend, 0, 0);
        dmxfd = I.dvb_dmx_open(adapter, demux);
    }

    public void setSys(LibDVBv5.fe_delivery_system sys) {
        int err = I.dvb_set_compat_delivery_system(params, sys.ordinal());
        if (err < 0) throw new RuntimeException("returned " + err);
    }

    public void tune(
            int                                              freqency   ,
            int                                              symbolReate,
            dvb_frontend_parameters.fe_modulation            modulation ,
            dvb_frontend_parameters.dvbfe_spectral_inversion inversion  ,
            dvb_frontend_parameters.dvbfe_code_rate          fec
    ) {
        int err;
        err = I.dvb_fe_store_parm(params, LibDVBv5.DTV_FREQUENCY  , freqency            ); if (err < 0) throw new RuntimeException("returned " + err);
        err = I.dvb_fe_store_parm(params, LibDVBv5.DTV_SYMBOL_RATE, symbolReate         ); if (err < 0) throw new RuntimeException("returned " + err);
        err = I.dvb_fe_store_parm(params, LibDVBv5.DTV_MODULATION , modulation.ordinal()); if (err < 0) throw new RuntimeException("returned " + err);
        err = I.dvb_fe_store_parm(params, LibDVBv5.DTV_INVERSION  , inversion .ordinal()); if (err < 0) throw new RuntimeException("returned " + err);
        err = I.dvb_fe_store_parm(params, LibDVBv5.DTV_INNER_FEC  , fec       .ordinal()); if (err < 0) throw new RuntimeException("returned " + err);
        err = I.dvb_fe_set_parms(params);                                                  if (err < 0) throw new RuntimeException("returned " + err);
    }

    /**
     *
     * @param pid Program ID to filter. Use 0x2000 to select all PIDs
     */
    public void filter(int pid) {
        int err = I.dvb_set_pesfilter(dmxfd, pid, dmx_pes_filter_params.dmx_ts_pes.DMX_PES_OTHER.ordinal(), dmx_pes_filter_params.dmx_output.DMX_OUT_TS_TAP.ordinal(), 65536);
        if (err < 0) throw new RuntimeException("returned " + err);
        err = I.dvb_set_section_filter(dmxfd, pid, 0,null,null,null, dmx_sct_filter_params.DMX_CHECK_CRC | dmx_sct_filter_params.DMX_IMMEDIATE_START);
        if (err < 0) throw new RuntimeException("returned " + err);
    }
}
