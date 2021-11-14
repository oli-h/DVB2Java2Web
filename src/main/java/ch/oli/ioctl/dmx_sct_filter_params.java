package ch.oli.ioctl;

import com.sun.jna.Structure;

@Structure.FieldOrder({"pid","filter","mask","mode","timeout","flags"})
public class dmx_sct_filter_params extends Structure {
    public short  pid;
    public byte[] filter = new byte[16];
    public byte[] mask   = new byte[16];
    public byte[] mode   = new byte[16];
    public int    timeout; // in millis
    public int    flags;

    // Used in flags
    public static final int DMX_CHECK_CRC       = 1; // only deliver sections where the CRC check succeeded
    public static final int DMX_ONESHOT         = 2; // disable the section filter after one section has been delivered
    public static final int DMX_IMMEDIATE_START = 4; // Start filter immediately without requiring a DMX_START
}
