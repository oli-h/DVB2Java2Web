package ch.oli.ioctl;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface LibC extends Library  {
    LibC x = Native.load(null, LibC.class);

    int O_RDONLY = 0;
    int O_WRONLY = 1;
    int O_RDWR = 2;

    // void printf(String format, Object... args);
    int open(String filename,int flags);
    int ioctl(int fd, long request, byte[] buf);
}
