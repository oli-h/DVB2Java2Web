package ch.oli.web;

import ch.oli.decode.MyBAOS;
import ch.oli.ioctl.DevDvbDemux;

import java.io.IOException;
import java.io.OutputStream;

public class H264Decoder {

    private final DevDvbDemux dmx;
    private final OutputStream os;

    public H264Decoder(DevDvbDemux dmx, OutputStream os) {
        this.dmx = dmx;
        this.os = os;
    }

    private final MyBAOS baos = new MyBAOS();

    public void decode() throws IOException {
        long lastBytes = 0;
        boolean currentBytesAreNal = false;
        boolean nal7foundOnce = false;
        while (true) {
            int b = dmx.file.readUnsignedByte();
            baos.write(b);
            lastBytes = (lastBytes << 8) | b;

            boolean pesHeaderFound = (lastBytes & 0xffff_ffff_ffffL) == 0x0000_01e0_0000L;
            boolean nalUnitHeaderFound = (lastBytes & 0xffff_ffffL) == 0x0000_0001L;

            if (!pesHeaderFound && !nalUnitHeaderFound) {
                continue;
            }
            // ... we found header-sync-bytes-battern

            if (currentBytesAreNal) {
                // remove the currently found header-bytes from the buffer as they belong to the next PES-Packet-Header or NAL-Unit
                int nri = (baos.buf()[4] >> 5) & 3;
                int nalUnitType = baos.buf()[4] & 0x1F;
                if (nalUnitType == 7) {
                    nal7foundOnce = true;
                }
                if (nal7foundOnce) {
                    System.out.printf("NAL Unit NRI=%d Type=%02x len=%s\n", nri, nalUnitType, baos.size());
                    if (pesHeaderFound) {
                        baos.removeBytesFromTail(6);
                    } else { // nalUnitHeaderFoundis true
                        baos.removeBytesFromTail(4);
                    }
                    baos.writeTo(os);
                }
                currentBytesAreNal = false;
            }

            if (nalUnitHeaderFound) {
                currentBytesAreNal = true;
                baos.reset();
                baos.write(0);
                baos.write(0);
                baos.write(0);
                baos.write(1);

            } else { // pesHeaderFound is true
                dmx.file.readUnsignedShort();  // PES Header Flags
                int pesHeadLen = dmx.file.readUnsignedByte(); // PES Header Length
                for (int i = 0; i < pesHeadLen; i++) {  // skip rest of PES Header
                    dmx.file.readUnsignedByte();
                }
            }
        }

    }
}
