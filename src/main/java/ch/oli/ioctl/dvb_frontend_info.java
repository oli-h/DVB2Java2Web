package ch.oli.ioctl;

import com.sun.jna.FromNativeContext;
import com.sun.jna.NativeMapped;
import com.sun.jna.Structure;

@Structure.FieldOrder({
        "name", "feType",
        "frequency_min", "frequency_max", "frequency_stepsize", "frequency_tolerance",
        "symbol_rate_min", "symbol_rate_max", "symbol_rate_tolerance",
        "notifier_delay", "caps"
})
public class dvb_frontend_info extends Structure {

    enum FeType implements NativeMapped {
        FE_QPSK, FE_QAM, FE_OFDM, FE_ATSC;

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

    public final static int FE_IS_STUPID = 0;
    enum CAPS_BITS {
        FE_CAN_INVERSION_AUTO        , // 0x1
        FE_CAN_FEC_1_2               , // 0x2
        FE_CAN_FEC_2_3               , // 0x4
        FE_CAN_FEC_3_4               , // 0x8
        FE_CAN_FEC_4_5               , // 0x10
        FE_CAN_FEC_5_6               , // 0x20
        FE_CAN_FEC_6_7               , // 0x40
        FE_CAN_FEC_7_8               , // 0x80
        FE_CAN_FEC_8_9               , // 0x100
        FE_CAN_FEC_AUTO              , // 0x200
        FE_CAN_QPSK                  , // 0x400
        FE_CAN_QAM_16                , // 0x800
        FE_CAN_QAM_32                , // 0x1000
        FE_CAN_QAM_64                , // 0x2000
        FE_CAN_QAM_128               , // 0x4000
        FE_CAN_QAM_256               , // 0x8000
        FE_CAN_QAM_AUTO              , // 0x10000
        FE_CAN_TRANSMISSION_MODE_AUTO, // 0x20000
        FE_CAN_BANDWIDTH_AUTO        , // 0x40000
        FE_CAN_GUARD_INTERVAL_AUTO   , // 0x80000
        FE_CAN_HIERARCHY_AUTO        , // 0x100000
        FE_CAN_8VSB                  , // 0x200000
        FE_CAN_16VSB                 , // 0x400000
        FE_HAS_EXTENDED_CAPS         , // 0x800000
        FE_UNUSED_1                  , // 0x1000000
        FE_UNUSED_2                  , // 0x2000000
        FE_CAN_MULTISTREAM           , // 0x4000000
        FE_CAN_TURBO_FEC             , // 0x8000000
        FE_CAN_2G_MODULATION         , // 0x10000000
        FE_NEEDS_BENDING             , // 0x20000000
        FE_CAN_RECOVER               , // 0x40000000
        FE_CAN_MUTE_TS               , // 0x80000000
    }

    public byte[] name = new byte[128]; // char[128]
    public FeType feType              ; // DEPRECATED. Use DTV_ENUM_DELSYS instead
    public int frequency_min          ;
    public int frequency_max          ;
    public int frequency_stepsize     ;
    public int frequency_tolerance    ;
    public int symbol_rate_min        ;
    public int symbol_rate_max        ;
    public int symbol_rate_tolerance  ;
    public int notifier_delay         ; // DEPRECATED
    public int caps                   ;

    public void getViaIoctl(int fdFrontend) {
        int FE_GET_INFO = 61;
        C.ioctl(fdFrontend, C.DIR.read, FE_GET_INFO, this);
    }

}
