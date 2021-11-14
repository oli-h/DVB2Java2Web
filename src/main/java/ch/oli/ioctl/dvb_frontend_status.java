package ch.oli.ioctl;

import com.sun.jna.Structure;

@Structure.FieldOrder("status")
public class dvb_frontend_status extends Structure {

    public static final int FE_NONE = 0x00;
    enum fe_status {
        FE_HAS_SIGNAL  , // 0x01,
        FE_HAS_CARRIER , // 0x02,
        FE_HAS_VITERBI , // 0x04,
        FE_HAS_SYNC    , // 0x08,
        FE_HAS_LOCK    , // 0x10,
        FE_TIMEDOUT    , // 0x20,
        FE_REINIT      , // 0x40,
    };

    public int status;

    public void getViaIoctl(int fdFrontend) {
        int FE_READ_STATUS = 69;
        C.ioctl(fdFrontend, C.DIR.read, FE_READ_STATUS, this);
    }

}
