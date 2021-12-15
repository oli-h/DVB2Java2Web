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
        boolean sync = false;
        boolean blubb = false;
        while (true) {
            // wait for start-code "0x000001e0" plus "0000" for pesLength=0
            // see http://mugenryodan.blogspot.com/2012/07/mpeg1mpeg2-start-code-table.html
            while (true) {
                int b = dmx.file.readUnsignedByte();
                baos.write(b);

                lastBytes = (lastBytes << 8) | b;
                boolean pesHeaderFound = (lastBytes & 0xffff_ffff_ffffL) == 0x0000_01e0_0000L;
                boolean nalUnitHeaderFound = (lastBytes & 0xffff_ffffL) == 0x0000_0001L;
                if (pesHeaderFound | nalUnitHeaderFound) {
//                    System.out.printf("%12x",lastBytes);
                    if (sync) {
                        if (pesHeaderFound) {
                            baos.removeBytesFromTail(6);
                        } else if (nalUnitHeaderFound) {
                            baos.removeBytesFromTail(4);
                        }
                        int nri = (baos.buf()[4]>>5) & 3;
                        int nalUnitType = baos.buf()[4] & 0x1F;
                        if (nalUnitType == 7) {
                            blubb = true;
                        }
                        if (blubb) {
                            System.out.printf("NAL Unit NRI=%d Type=%02x len=%s\n", nri, nalUnitType, baos.size());
                            baos.writeTo(os);
                        }
                    }
                    sync = true;
                    baos.reset();
                    if (nalUnitHeaderFound) {
                        baos.write(0);
                        baos.write(0);
                        baos.write(0);
                        baos.write(1);
                    }
                    if (pesHeaderFound) {
                        dmx.file.readUnsignedShort();  // PES Header Flags
                        int pesHeadLen = dmx.file.readUnsignedByte(); // PES Header Length
                        for (int i = 0; i < pesHeadLen; i++) {  // skip rest of PES Header
                            dmx.file.readUnsignedByte();
                        }
                    }
                }
            }
        }
    }
}
