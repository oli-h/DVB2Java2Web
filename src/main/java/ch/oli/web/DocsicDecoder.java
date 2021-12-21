package ch.oli.web;

import ch.oli.ioctl.DevDvbDemux;

import java.io.IOException;
import java.io.OutputStream;

public class DocsicDecoder {

    private final DevDvbDemux dmx;
    private final OutputStream os;

    public DocsicDecoder(DevDvbDemux dmx, OutputStream os) {
        this.dmx = dmx;
        this.os = os;
    }

    public void decode() throws IOException {
        int x = 0;
        while (true) {
            int b = dmx.file.readUnsignedByte();
            System.out.printf("%02x ", b);
            x++;
            if (x == 32) {
                System.out.println();
                x = 0;
                os.write(1);
                os.flush();
            }
        }
    }
}
