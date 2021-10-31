package ch.oli.ioctl;

import com.sun.jna.Native;

public class IoctlBase {

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

    private final byte[] buf;

    protected IoctlBase(int size) {
        // 'size' is to be reengineered from C-struct-sizes
        buf = new byte[size];
    }

    /**
     * @param fd  file descriptor (handle) to e.g "/dev/dvb/adapter0/frontend0"
     * @param dir 0=none, 1=read, 2=write, 3=read/write
     * @param nr  the IOCTL-NUmber
     */
    protected void doIoctl(int fd, DIR dir, int nr) {
        // see https://www.kernel.org/doc/html/latest/userspace-api/ioctl/ioctl-number.html
        // 'o' is for all DVB-Stuff
        long request =              dir.ordinal();
        request = (request << 14) | buf.length   ;
        request = (request <<  8) | 'o'          ;
        request = (request <<  8) | nr           ;

        int ioctl = LibC.x.ioctl(fd, request, buf);
        if (ioctl != 0) {
            errnoToException();
        }
    }

    protected int int32(int idx) {
        return ((buf[idx + 3] & 0xFF) << 24) | ((buf[idx + 2] & 0xFF) << 16) | ((buf[idx + 1] & 0xFF) << 8) | (buf[idx] & 0xFF);
    }

    protected void int32(int idx, int value) {
        buf[idx    ] = (byte)  value       ;
        buf[idx + 1] = (byte) (value >>  8);
        buf[idx + 2] = (byte) (value >> 16);
        buf[idx + 3] = (byte) (value >> 24);
    }

    protected String string(int idx, int length) {
        return new String(buf, idx, length);
    }

    protected void errnoToException() {
        throw new RuntimeException("errno=" + Native.getLastError());
    }
}
