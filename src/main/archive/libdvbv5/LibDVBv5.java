package ch.oli.libdvbv5;

import ch.oli.ioctl.C;
import com.sun.jna.*;
import com.sun.jna.ptr.IntByReference;

public interface LibDVBv5 extends Library {
    LibDVBv5 INSTANCE = Native.load("dvbv5", LibDVBv5.class);

    dvb_v5_fe_parms dvb_fe_open(int adapter, int frontend, int verbose, int use_legacy_call);
    int dvb_set_sys(dvb_v5_fe_parms parms, C.fe_delivery_system sys);
    int dvb_set_compat_delivery_system(dvb_v5_fe_parms parms, int sys);

    int dvb_fe_store_parm(dvb_v5_fe_parms parms, int cmd, int value);
    int dvb_fe_set_parms(dvb_v5_fe_parms parms);
    int dvb_fe_get_parms(dvb_v5_fe_parms parms);
    int dvb_fe_retrieve_parm(dvb_v5_fe_parms parms, int cmd, IntByReference value);

    int DTV_STAT_COMMAND_START = 512;
    int DTV_STATUS             = (DTV_STAT_COMMAND_START + 0);
    int DTV_BER                = (DTV_STAT_COMMAND_START + 1);
    int DTV_PER                = (DTV_STAT_COMMAND_START + 2);
    int DTV_QUALITY            = (DTV_STAT_COMMAND_START + 3);
    int DTV_PRE_BER            = (DTV_STAT_COMMAND_START + 4);
    int DTV_MAX_STAT_COMMAND   = DTV_PRE_BER;

    /**
     * Updates the stats cache from the available stats at the Kernel
     */
    int dvb_fe_get_stats(dvb_v5_fe_parms parms);

    /**
     * That's similar of calling both dvb_fe_get_parms() and dvb_fe_get_stats().
     */
    int dvb_fe_get_event(dvb_v5_fe_parms parms);
    int dvb_fe_retrieve_stats(dvb_v5_fe_parms parms, int cmd, IntByReference value);

    @Structure.FieldOrder({
            "info", "version", "has_v5_stats", "current_sys", "num_systems", "systems", "legacy_fe", "abort",
            "lna", "lnb", "sat_number", "freq_bpf", "diseqc_wait", "verbose", "logfunc", "default_charset", "output_charset"
    })
    class dvb_v5_fe_parms extends Structure {

        public dvb_frontend_info info;
        public int version;
        public int has_v5_stats;
        public C.fe_delivery_system current_sys;
        public int num_systems;
        public C.fe_delivery_system[] systems = new C.fe_delivery_system[10]; // hm... should be 20 - but works with 10
        public int legacy_fe;

        /* The values below are specified by the library client */

        /* Flags from the client to the library */
        public int abort;

        /* Linear Amplifier settings */
        public int lna;

        /* Satellite settings */
        public dvb_sat_lnb lnb;
        public int sat_number;
        public int freq_bpf;
        public int diseqc_wait;

        /* Function to write DVB logs */
        public int verbose;
        public Pointer logfunc;

        /* Charsets to be used by the conversion utilities */
        public String default_charset;
        public String output_charset;
    }

    @Structure.FieldOrder({
            "nameNative", "type", "frequency_min", "frequency_max", "frequency_stepsize", "frequency_tolerance", "symbol_rate_min",
            "symbol_rate_max", "symbol_rate_tolerance", "notifier_delay", "caps"
    })
    class dvb_frontend_info extends Structure {
        public byte[] nameNative = new byte[128];
        private String name;
        public fe_type type;      /* DEPRECATED. Use DTV_ENUM_DELSYS instead */
        public int frequency_min;
        public int frequency_max;
        public int frequency_stepsize;
        public int frequency_tolerance;
        public int symbol_rate_min;
        public int symbol_rate_max;
        public int symbol_rate_tolerance;
        public int notifier_delay;              /* DEPRECATED */
        public int caps;

        @Override
        public void read() {
            super.read();
            name = new String(nameNative);
        }
    }

    enum fe_type implements NativeMapped {
        FE_QPSK,
        FE_QAM,
        FE_OFDM,
        FE_ATSC;

        @Override
        public Object fromNative(Object nativeValue, FromNativeContext context) {
            return values()[(Integer) nativeValue];
        }

        @Override
        public Object toNative() {
            return this.ordinal();
        }

        @Override
        public Class<?> nativeType() {
            return Integer.class;
        }
    }


    @Structure.FieldOrder({"name", "alias", "lowfreq", "highfreq", "rangeswitch", "low1", "high1", "low2", "high2"})
    class dvb_sat_lnb extends Structure {
        public String name;
        public String alias;

        /*
         * Legacy fields, kept just to avoid ABI breakages
         * Should not be used by new applications
         */
        public int lowfreq, highfreq;
        public int rangeswitch;
        public int low1, high1;
        public int low2, high2;
    }


    /**
     * @return file descriptor on success, -1 otherwise.
     */
    int dvb_dmx_open(int adapter, int demux);

    /**
     *
     * @param dmxfd       File descriptor for the demux device
     * @param pid         Program ID to filter. Use 0x2000 to select all PIDs
     * @param type        ype of the PID (DMX_PES_VIDEO, DMX_PES_AUDIO, DMX_PES_OTHER, etc)
     * @param output      Where the data will be output (DMX_OUT_TS_TAP, DMX_OUT_DECODER, etc).
     * @param buffersize Size of the buffer to be allocated to store the filtered data
     * @return zero on success, -1 otherwise.
     */
    int dvb_set_pesfilter(int dmxfd, int pid, int type, int output, int buffersize);

    /**
     *
     * @param dmxfd    File descriptor for the demux device
     * @param pid      Program ID to filter. Use 0x2000 to select all PIDs
     * @param filtsize Size of the filter (up to 18 btyes)
     * @param filter   data to filter. Can be NULL or should have filtsize length
     * @param mask     filter mask. Can be NULL or should have filtsize length
     * @param mode     mode mask. Can be NULL or should have filtsize length
     * @param flags    flags for set filter (DMX_CHECK_CRC,DMX_ONESHOT, DMX_IMMEDIATE_START).
     * @return         zero on success, -1 otherwise.
     */
    int dvb_set_section_filter(int dmxfd, int pid,
                               int filtsize, byte[] filter, byte[] mask, byte[] mode,
                               int flags);
}
