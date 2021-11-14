package ch.oli.libdvbv5;

import com.sun.jna.*;
import com.sun.jna.ptr.IntByReference;

public interface LibDVBv5 extends Library {
    LibDVBv5 INSTANCE = Native.load("dvbv5", LibDVBv5.class);

    dvb_v5_fe_parms dvb_fe_open(int adapter, int frontend, int verbose, int use_legacy_call);
    int dvb_set_sys(dvb_v5_fe_parms parms, fe_delivery_system sys);
    int dvb_set_compat_delivery_system(dvb_v5_fe_parms parms, int sys);

    /* DVBv5 property Commands */
    int DTV_UNDEFINED          =  0;
    int DTV_TUNE               =  1;
    int DTV_CLEAR              =  2;
    int DTV_FREQUENCY          =  3;
    int DTV_MODULATION         =  4;
    int DTV_BANDWIDTH_HZ       =  5;
    int DTV_INVERSION          =  6;
    int DTV_DISEQC_MASTER      =  7;
    int DTV_SYMBOL_RATE        =  8;
    int DTV_INNER_FEC          =  9;
    int DTV_VOLTAGE            = 10;
    int DTV_TONE               = 11;
    int DTV_PILOT              = 12;
    int DTV_ROLLOFF            = 13;
    int DTV_DISEQC_SLAVE_REPLY = 14;
    int dvb_fe_store_parm(dvb_v5_fe_parms parms, int cmd, int value);
    int dvb_fe_set_parms(dvb_v5_fe_parms parms);
    int dvb_fe_get_parms(dvb_v5_fe_parms parms);
    int dvb_fe_retrieve_parm(dvb_v5_fe_parms parms, int cmd, IntByReference value);

    /* Basic enumeration set for querying unlimited capabilities */
    int DTV_FE_CAPABILITY_COUNT = 15;
    int DTV_FE_CAPABILITY       = 16;
    int DTV_DELIVERY_SYSTEM     = 17;

    /* ISDB-T and ISDB-Tsb */
    int DTV_ISDBT_PARTIAL_RECEPTION  = 18;
    int DTV_ISDBT_SOUND_BROADCASTING = 19;

    int DTV_ISDBT_SB_SUBCHANNEL_ID   = 20;
    int DTV_ISDBT_SB_SEGMENT_IDX     = 21;
    int DTV_ISDBT_SB_SEGMENT_COUNT   = 22;

    int DTV_ISDBT_LAYERA_FEC               = 23;
    int DTV_ISDBT_LAYERA_MODULATION        = 24;
    int DTV_ISDBT_LAYERA_SEGMENT_COUNT     = 25;
    int DTV_ISDBT_LAYERA_TIME_INTERLEAVING = 26;
    int DTV_ISDBT_LAYERB_FEC               = 27;
    int DTV_ISDBT_LAYERB_MODULATION        = 28;
    int DTV_ISDBT_LAYERB_SEGMENT_COUNT     = 29;
    int DTV_ISDBT_LAYERB_TIME_INTERLEAVING = 30;
    int DTV_ISDBT_LAYERC_FEC               = 31;
    int DTV_ISDBT_LAYERC_MODULATION        = 32;
    int DTV_ISDBT_LAYERC_SEGMENT_COUNT     = 33;
    int DTV_ISDBT_LAYERC_TIME_INTERLEAVING = 34;

    int DTV_API_VERSION = 35;

    int DTV_CODE_RATE_HP        = 36;
    int DTV_CODE_RATE_LP        = 37;
    int DTV_GUARD_INTERVAL      = 38;
    int DTV_TRANSMISSION_MODE   = 39;
    int DTV_HIERARCHY           = 40;

    int DTV_ISDBT_LAYER_ENABLED = 41;

    int DTV_STREAM_ID           = 42;
    int DTV_ISDBS_TS_ID_LEGACY  = DTV_STREAM_ID;
    int DTV_DVBT2_PLP_ID_LEGACY = 43;

    int DTV_ENUM_DELSYS         = 44;

    /* ATSC-MH */
    int DTV_ATSCMH_FIC_VER           = 45;
    int DTV_ATSCMH_PARADE_ID         = 46;
    int DTV_ATSCMH_NOG               = 47;
    int DTV_ATSCMH_TNOG              = 48;
    int DTV_ATSCMH_SGN               = 49;
    int DTV_ATSCMH_PRC               = 50;
    int DTV_ATSCMH_RS_FRAME_MODE     = 51;
    int DTV_ATSCMH_RS_FRAME_ENSEMBLE = 52;
    int DTV_ATSCMH_RS_CODE_MODE_PRI  = 53;
    int DTV_ATSCMH_RS_CODE_MODE_SEC  = 54;
    int DTV_ATSCMH_SCCC_BLOCK_MODE   = 55;
    int DTV_ATSCMH_SCCC_CODE_MODE_A  = 56;
    int DTV_ATSCMH_SCCC_CODE_MODE_B  = 57;
    int DTV_ATSCMH_SCCC_CODE_MODE_C  = 58;
    int DTV_ATSCMH_SCCC_CODE_MODE_D  = 59;

    int DTV_INTERLEAVING = 60;
    int DTV_LNA          = 61;

    /* Quality parameters */
    int DTV_STAT_SIGNAL_STRENGTH        = 62;
    int DTV_STAT_CNR                    = 63; // Signal to Noise ratio for the main carrier.
    int DTV_STAT_PRE_ERROR_BIT_COUNT    = 64;
    int DTV_STAT_PRE_TOTAL_BIT_COUNT    = 65;
    int DTV_STAT_POST_ERROR_BIT_COUNT   = 66;
    int DTV_STAT_POST_TOTAL_BIT_COUNT   = 67;
    int DTV_STAT_ERROR_BLOCK_COUNT      = 68;
    int DTV_STAT_TOTAL_BLOCK_COUNT      = 69;

    /* Physical layer scrambling */
    int DTV_SCRAMBLING_SEQUENCE_INDEX = 70;
    int DTV_MAX_COMMAND = DTV_SCRAMBLING_SEQUENCE_INDEX;

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
        public fe_delivery_system current_sys;
        public int num_systems;
        public fe_delivery_system[] systems = new fe_delivery_system[10]; // hm... should be 20 - but works with 10
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


    enum fe_delivery_system implements NativeMapped {
        SYS_UNDEFINED,
        SYS_DVBC_ANNEX_A,
        SYS_DVBC_ANNEX_B,
        SYS_DVBT,
        SYS_DSS,
        SYS_DVBS,
        SYS_DVBS2,
        SYS_DVBH,
        SYS_ISDBT,
        SYS_ISDBS,
        SYS_ISDBC,
        SYS_ATSC,
        SYS_ATSCMH,
        SYS_DTMB,
        SYS_CMMB,
        SYS_DAB,
        SYS_DVBT2,
        SYS_TURBO,
        SYS_DVBC_ANNEX_C,
        ;

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
