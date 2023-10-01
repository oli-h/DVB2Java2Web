package ch.oli.ioctl;

import java.io.Closeable;

public class DevDvbFrontend implements Closeable {
    public final String devAdapter;
    private final int fdFrontend;

    public DevDvbFrontend(int adapter) {
        devAdapter = "/dev/dvb/adapter" + adapter;
        fdFrontend = LibC.x.open(devAdapter+ "/frontend0", LibC.O_RDWR);
    }

    @Override
    public void close() {
        LibC.x.close(fdFrontend);
    }

    public dvb_frontend_info feGetInfo() {
        dvb_frontend_info fei = new dvb_frontend_info();
        final int FE_GET_INFO = 61;
        C.ioctl(fdFrontend, C.DIR.read, FE_GET_INFO, fei);
        return fei;
    }

//    public void feSetFrontend(dvb_frontend_parameters fep) {
//        final int FE_SET_FRONTEND = 76;
//        C.ioctl(fdFrontend, C.DIR.write, FE_SET_FRONTEND, fep);
//    }

    public void feSetProperty(dtv_properties props) {
        final int FE_SET_PROPERTY = 82;
        C.ioctl(fdFrontend, C.DIR.write, FE_SET_PROPERTY, props);
    }

    public void feGetProperty(dtv_properties props) {
        final int FE_GET_PROPERTY = 83;
        C.ioctl(fdFrontend, C.DIR.read, FE_GET_PROPERTY, props);
    }

    public int feReadStatus() {
        dvb_frontend_status fes = new dvb_frontend_status();
        int FE_READ_STATUS = 69;
        C.ioctl(fdFrontend, C.DIR.read, FE_READ_STATUS, fes);
        return fes.status;
    }

    public DevDvbDemux openDemux() {
        return new DevDvbDemux(devAdapter);
    }

}
