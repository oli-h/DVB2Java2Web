package ch.oli.ioctl;

import java.io.*;

public class DevDvbDemux implements Closeable {
    private final int fdDemux;
    public DataInputStream file;

    protected DevDvbDemux(String devAdapter) {
        fdDemux = LibC.x.open(devAdapter+ "/demux0", LibC.O_RDONLY);
        file = new DataInputStream(new InputStream() {
            byte[] buffer = new byte[65536];

            @Override
            public int read() {
                int numRead = LibC.x.read(fdDemux, buffer, 1);
                if (numRead == 0) {
                    return -1;
                }
                return buffer[0] & 0xFF;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int numRead = LibC.x.read(fdDemux, buffer, Math.min(buffer.length, len));
                if (numRead == 0) {
                    return -1;
                }
                System.arraycopy(buffer, 0, b, off, numRead);
                return numRead;
            }
        });
    }

    @Override
    public void close() {
        LibC.x.close(fdDemux);
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
