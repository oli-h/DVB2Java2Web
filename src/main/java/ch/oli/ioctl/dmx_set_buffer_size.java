package ch.oli.ioctl;

public class dmx_set_buffer_size {

    public long size;

    public void setViaIoctl(int fdDemux) {
        final int DMX_SET_BUFFER_SIZE = 45;
        C.ioctl(fdDemux, C.DIR.none, DMX_SET_BUFFER_SIZE, size);
    }
}
