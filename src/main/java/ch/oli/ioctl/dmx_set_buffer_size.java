package ch.oli.ioctl;

public class dmx_set_buffer_size extends IoctlBase {

    protected dmx_set_buffer_size() {
        super(0);
    }

    public long size;

    public void setViaIoctl(int fdDemux) {
        final int DMX_SET_BUFFER_SIZE = 45;
        doIoctl(fdDemux, DIR.none, DMX_SET_BUFFER_SIZE, size);
    }
}
