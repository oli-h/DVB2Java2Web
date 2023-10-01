package ch.oli.ioctl;

import com.sun.jna.FromNativeContext;
import com.sun.jna.NativeMapped;
import com.sun.jna.Structure;

public class C {

    public enum fe_delivery_system implements NativeMapped {
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
        SYS_DVBC_ANNEX_C;

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

    public enum fe_modulation implements NativeMapped {
        QPSK  ,                                              // 0
        QAM_16, QAM_32 , QAM_64, QAM_128, QAM_256, QAM_AUTO, // 1..6
        VSB_8 , VSB_16 ,                                     // 7..8
        PSK_8 , APSK_16, APSK_32,                            // 9..11
        DQPSK ,                                              // 12
        QAM_4_NR;                                            // 13

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

    public enum dvbfe_spectral_inversion implements NativeMapped {
        DVBFE_INVERSION_OFF,
        DVBFE_INVERSION_ON,
        DVBFE_INVERSION_AUTO;

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

    public enum dvbfe_code_rate implements NativeMapped {
        DVBFE_FEC_NONE,
        DVBFE_FEC_1_2,
        DVBFE_FEC_2_3,
        DVBFE_FEC_3_4,
        DVBFE_FEC_4_5,
        DVBFE_FEC_5_6,
        DVBFE_FEC_6_7,
        DVBFE_FEC_7_8,
        DVBFE_FEC_8_9,
        DVBFE_FEC_AUTO;

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

    public enum CMD implements NativeMapped {
        /* DVBv5 property Commands */
        DTV_UNDEFINED               , //  0
        DTV_TUNE                    , //  1
        DTV_CLEAR                   , //  2
        DTV_FREQUENCY               , //  3
        DTV_MODULATION              , //  4
        DTV_BANDWIDTH_HZ            , //  5
        DTV_INVERSION               , //  6
        DTV_DISEQC_MASTER           , //  7
        DTV_SYMBOL_RATE             , //  8
        DTV_INNER_FEC               , //  9
        DTV_VOLTAGE                 , // 10
        DTV_TONE                    , // 11
        DTV_PILOT                   , // 12
        DTV_ROLLOFF                 , // 13
        DTV_DISEQC_SLAVE_REPLY      , // 14

        /* Basic enumeration set for querying unlimited capabilities */
        DTV_FE_CAPABILITY_COUNT     , // 15
        DTV_FE_CAPABILITY           , // 16
        DTV_DELIVERY_SYSTEM         , // 17

        /* ISDB-T and ISDB-Tsb */
        DTV_ISDBT_PARTIAL_RECEPTION , // 18
        DTV_ISDBT_SOUND_BROADCASTING, // 19

        DTV_ISDBT_SB_SUBCHANNEL_ID  , // 20
        DTV_ISDBT_SB_SEGMENT_IDX    , // 21
        DTV_ISDBT_SB_SEGMENT_COUNT  , // 22

        DTV_ISDBT_LAYERA_FEC              , // 23
        DTV_ISDBT_LAYERA_MODULATION       , // 24
        DTV_ISDBT_LAYERA_SEGMENT_COUNT    , // 25
        DTV_ISDBT_LAYERA_TIME_INTERLEAVING, // 26
        DTV_ISDBT_LAYERB_FEC              , // 27
        DTV_ISDBT_LAYERB_MODULATION       , // 28
        DTV_ISDBT_LAYERB_SEGMENT_COUNT    , // 29
        DTV_ISDBT_LAYERB_TIME_INTERLEAVING, // 30
        DTV_ISDBT_LAYERC_FEC              , // 31
        DTV_ISDBT_LAYERC_MODULATION       , // 32
        DTV_ISDBT_LAYERC_SEGMENT_COUNT    , // 33
        DTV_ISDBT_LAYERC_TIME_INTERLEAVING, // 34

        DTV_API_VERSION                   , // 35;

        DTV_CODE_RATE_HP                  , // 36;
        DTV_CODE_RATE_LP                  , // 37;
        DTV_GUARD_INTERVAL                , // 38;
        DTV_TRANSMISSION_MODE             , // 39;
        DTV_HIERARCHY                     , // 40;

        DTV_ISDBT_LAYER_ENABLED           , // 41

        DTV_STREAM_ID                     , // 42 (aka DTV_ISDBS_TS_ID_LEGACY)
        DTV_DVBT2_PLP_ID_LEGACY           , // 43

        DTV_ENUM_DELSYS                   , // 44

        /* ATSC-MH */
        DTV_ATSCMH_FIC_VER                , // 45
        DTV_ATSCMH_PARADE_ID              , // 46
        DTV_ATSCMH_NOG                    , // 47
        DTV_ATSCMH_TNOG                   , // 48
        DTV_ATSCMH_SGN                    , // 49
        DTV_ATSCMH_PRC                    , // 50
        DTV_ATSCMH_RS_FRAME_MODE          , // 51
        DTV_ATSCMH_RS_FRAME_ENSEMBLE      , // 52
        DTV_ATSCMH_RS_CODE_MODE_PRI       , // 53
        DTV_ATSCMH_RS_CODE_MODE_SEC       , // 54
        DTV_ATSCMH_SCCC_BLOCK_MODE        , // 55
        DTV_ATSCMH_SCCC_CODE_MODE_A       , // 56
        DTV_ATSCMH_SCCC_CODE_MODE_B       , // 57
        DTV_ATSCMH_SCCC_CODE_MODE_C       , // 58
        DTV_ATSCMH_SCCC_CODE_MODE_D       , // 59

        DTV_INTERLEAVING                  , // 60
        DTV_LNA                           , // 61

        /* Quality parameters */
        DTV_STAT_SIGNAL_STRENGTH          , // 62
        DTV_STAT_CNR                      , // 63 Signal to Noise ratio for the main carrier.
        DTV_STAT_PRE_ERROR_BIT_COUNT      , // 64
        DTV_STAT_PRE_TOTAL_BIT_COUNT      , // 65
        DTV_STAT_POST_ERROR_BIT_COUNT     , // 66
        DTV_STAT_POST_TOTAL_BIT_COUNT     , // 67
        DTV_STAT_ERROR_BLOCK_COUNT        , // 68
        DTV_STAT_TOTAL_BLOCK_COUNT        , // 69

        /* Physical layer scrambling */
        DTV_SCRAMBLING_SEQUENCE_INDEX     ; // 70

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
    int DTV_MAX_COMMAND = CMD.DTV_SCRAMBLING_SEQUENCE_INDEX.ordinal(); // assume = 70









//    private static final int _IOC_NRBITS = 8;
//    private static final int _IOC_TYPEBITS = 8;
//    private static final int _IOC_SIZEBITS = 14;
//
//    private static final int _IOC_NRSHIFT = 0;
//    private static final int _IOC_TYPESHIFT = (_IOC_NRSHIFT + _IOC_NRBITS);
//    private static final int _IOC_SIZESHIFT = (_IOC_TYPESHIFT + _IOC_TYPEBITS);
//    private static final int _IOC_DIRSHIFT = (_IOC_SIZESHIFT + _IOC_SIZEBITS);

    public enum DIR {
        none, write, read, rdwt
    }

//    public static final long FE_GET_INFO     = (_IOC_READ  << _IOC_DIRSHIFT) | ('o' << _IOC_TYPESHIFT) | (61L << _IOC_NRSHIFT) | (168L << _IOC_SIZESHIFT);
//    public static final long FE_SET_FRONTEND = (_IOC_WRITE << _IOC_DIRSHIFT) | ('o' << _IOC_TYPESHIFT) | (76L << _IOC_NRSHIFT) | (36L << _IOC_SIZESHIFT);
//    public static final long FE_GET_FRONTEND = (_IOC_READ  << _IOC_DIRSHIFT) | ('o' << _IOC_TYPESHIFT) | (77L << _IOC_NRSHIFT) | (36L << _IOC_SIZESHIFT);

    /**
     * @param fd  file descriptor (handle) to e.g "/dev/dvb/adapter0/frontend0"
     * @param dir 0=none, 1=read, 2=write, 3=read/write
     * @param nr  the IOCTL-NUmber
     */
    public static void ioctl(int fd, DIR dir, int nr, Object value) {
        int size;
        if (value instanceof Structure) {
            size = ((Structure) value).size();
        } else if (value instanceof Long) {
            size = 0;
        } else if (value instanceof byte[]) {
            size = ((byte[]) value).length;
        } else {
            throw new RuntimeException("Can't get size for " + value);
        }

        if (dir == DIR.none && size > 0) {
            throw new RuntimeException("Hm? buf.length shall be 0 when dir is  'none'");
        }
        if (dir != DIR.none && size == 0) {
            throw new RuntimeException("Hm? buf.length shall not be 0 when dir is '" + dir + "'");
        }

        // see https://www.kernel.org/doc/html/latest/userspace-api/ioctl/ioctl-number.html
        // 'o' is for all DVB-Stuff
        long request =              dir.ordinal();
        request = (request << 14) | size         ;
        request = (request <<  8) | 'o'          ;
        request = (request <<  8) | nr           ;

        LibC.x.ioctl(fd, request, value);
    }
}
