package ch.oli.libdvbv5;

import com.sun.jna.*;
import com.sun.jna.ptr.IntByReference;

public interface LibDVBv5 extends Library {
    LibDVBv5 INSTANCE = Native.load("dvbv5", LibDVBv5.class);

    dvb_v5_fe_parms dvb_fe_open(int adapter, int frontend, int verbose, int use_legacy_call);
    int dvb_set_sys(dvb_v5_fe_parms parms, fe_delivery_system sys);

    /* DVBv5 property Commands */
    int DTV_UNDEFINED           =0 ;
    int DTV_TUNE                =1 ;
    int DTV_CLEAR               =2 ;
    int DTV_FREQUENCY           =3 ;
    int DTV_MODULATION          =4 ;
    int DTV_BANDWIDTH_HZ        =5 ;
    int DTV_INVERSION           =6 ;
    int DTV_DISEQC_MASTER       =7 ;
    int DTV_SYMBOL_RATE         =8 ;
    int DTV_INNER_FEC           =9 ;
    int DTV_VOLTAGE             =10;
    int DTV_TONE                =11;
    int DTV_PILOT               =12;
    int DTV_ROLLOFF             =13;
    int DTV_DISEQC_SLAVE_REPLY  =14;
    int dvb_fe_store_parm(dvb_v5_fe_parms parms, int cmd, int value);
    int dvb_fe_set_parms(dvb_v5_fe_parms parms);
    int dvb_fe_get_stats(dvb_v5_fe_parms parms);

    /* Quality parameters */
    int DTV_STAT_SIGNAL_STRENGTH        = 62;
    int DTV_STAT_CNR                    = 63;
    int DTV_STAT_PRE_ERROR_BIT_COUNT    = 64;
    int DTV_STAT_PRE_TOTAL_BIT_COUNT    = 65;
    int DTV_STAT_POST_ERROR_BIT_COUNT   = 66;
    int DTV_STAT_POST_TOTAL_BIT_COUNT   = 67;
    int DTV_STAT_ERROR_BLOCK_COUNT      = 68;
    int DTV_STAT_TOTAL_BLOCK_COUNT      = 69;
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
}
