package ch.oli.ioctl;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface LibC extends Library {
    LibC x = Native.load(null, LibC.class);

    int O_RDONLY = 0;
    int O_WRONLY = 1;
    int O_RDWR = 2;

    /**
     * @param pathname the file (or directory)
     * @param flags at least one of O_RDONLY, O_WRONLY or O_RDWR
     * @return the new file descriptor (a nonnegative integer), or -1 if an error occurred (in which case, errno is set appropriately)
     */
    int open(String pathname, int flags);

    /**
     *
     * @param fd
     * @param buf
     * @param count
     * @return number of bytes read (zero indicates end of file)
     */
    int read(int fd, Object buf, long count);

    /**
     * @param fd the file descriptor to close
     * @return zero on success.  On error, -1 is returned, and errno is set appropriately.
     */
    int close(int fd);

    // void printf(String format, Object... args);
    int ioctl(int fd, long request, Object value);
}
