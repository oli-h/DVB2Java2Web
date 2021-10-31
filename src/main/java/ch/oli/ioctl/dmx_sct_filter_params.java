package ch.oli.ioctl;

public class dmx_sct_filter_params extends IoctlBase {

    protected dmx_sct_filter_params() {
        super(60);
    }

    // Used in flags
    public static final int DMX_CHECK_CRC       = 1; // only deliver sections where the CRC check succeeded
    public static final int DMX_ONESHOT         = 2; // disable the section filter after one section has been delivered
    public static final int DMX_IMMEDIATE_START = 4; // Start filter immediately without requiring a DMX_START

    public short pid;
    public byte[] filter = new byte[16];
    public byte[] mask   = new byte[16];
    public byte[] mode   = new byte[16];
    public int timeout; // in millis
    public int flags;

    public void setViaIoctl(int fdDemux) {
        int16(0, pid);
        System.arraycopy(filter, 0, buf,  4, 16);
        System.arraycopy(mask  , 0, buf, 20, 16);
        System.arraycopy(mode  , 0, buf, 36, 16);
        int32(52, timeout);
        int32(56, flags);

        final int DMX_SET_FILTER = 43;
        doIoctl(fdDemux, DIR.write, DMX_SET_FILTER, buf);
    }
}
