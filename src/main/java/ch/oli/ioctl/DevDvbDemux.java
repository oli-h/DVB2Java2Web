package ch.oli.ioctl;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;

public class DevDvbDemux implements Closeable {

    public final RandomAccessFile file;
    private final int fdDemux;

    protected DevDvbDemux(String devAdapter) {
        try {
            Field field = FileDescriptor.class.getDeclaredField("fd");
            field.setAccessible(true);
            file = new RandomAccessFile(devAdapter + "/demux0", "r");
            fdDemux = (int) field.get(file.getFD());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void close() {
        try {
            file.close();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void dmxSetBufferSize(long size) {
        final int DMX_SET_BUFFER_SIZE = 45;
        C.ioctl(fdDemux, C.DIR.none, DMX_SET_BUFFER_SIZE, size);
    }

    public void dmxSetFilter(dmx_sct_filter_params params) {
        final int DMX_SET_FILTER = 43;
        C.ioctl(fdDemux, C.DIR.write, DMX_SET_FILTER, params);
    }

    public void dmxSetPesFilter(dmx_pes_filter_params params) {
        final int DMX_SET_PES_FILTER = 44;
        C.ioctl(fdDemux, C.DIR.write, DMX_SET_PES_FILTER, params);
    }
}
