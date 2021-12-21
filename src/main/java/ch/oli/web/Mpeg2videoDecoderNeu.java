package ch.oli.web;

import ch.oli.decode.MyBAOS;
import ch.oli.ioctl.DevDvbDemux;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Mpeg2videoDecoderNeu {

    private final DevDvbDemux dmx;
    private final DataOutputStream os;

    public Mpeg2videoDecoderNeu(DevDvbDemux dmx, OutputStream os) {
        this.dmx = dmx;
        this.os = new DataOutputStream(os);
    }

    private final MyBAOS baos = new MyBAOS();

    public void decode() throws IOException {
        long t0 = System.currentTimeMillis();

        int lastBytes = 0;
        int startCode = 0;
        boolean currentBytesAreMpeg2video = false;
        boolean sequenceFound = false;

        while (true) {
            int b = dmx.file.readUnsignedByte();
            baos.write(b);
            lastBytes = (lastBytes << 8) | b;

            boolean headerFound = (lastBytes & 0xffff_ff00) == 0x0000_0100;
            if (!headerFound) {
                continue;
            }

            if (currentBytesAreMpeg2video && sequenceFound) {
                baos.removeBytesFromTail(4); // strip the (already buffered) bytes of _this_ header ...

                if (startCode >= 0x00000101 && startCode <= 0x000001af) {
                    // no print
//                    decodeSlice();
//                } else if(startCode==0x000001b5 || startCode==0x000001b2) {
                    // no print
                } else {
                    System.out.printf("0x%08X size=%4d: ", startCode, baos.size());
                    switch (startCode) {
                        case 0x00000100: decodePicture             (); break;
                        case 0x000001b5: decodeExtension           (); break;
                        case 0x000001b2: decodeUserData            (); break;
                        case 0x000001b3: decodeSequence            (); break;
                        case 0x000001b8: decodeGroupOfPictures     (); break;
                    }
                    System.out.println();
                    System.out.flush();
                }

                baos.writeTo(os);
            }

            baos.reset();
            baos.write(lastBytes >> 24); // ... but add _this_ header to the next buffer
            baos.write(lastBytes >> 16);
            baos.write(lastBytes >>  8);
            baos.write(lastBytes >>  0);
            startCode = lastBytes;

            if (lastBytes >= 0x000001e0 && lastBytes <= 0x000001ef) {
                currentBytesAreMpeg2video = false;
            } else {
                if (!sequenceFound && lastBytes == 0x000001b3) {
                    System.out.println("Time for first sequence start: " + (System.currentTimeMillis() - t0) + " ms");
                    sequenceFound = true;
                }
                currentBytesAreMpeg2video = true;
            }

        }
    }

    private void decodeSequence() {
        byte[] buf = baos.buf();
        int hSize = buf[4] & 0xFF;
        hSize <<= 4;
        int vSize = buf[5] & 0xFF;
        hSize |= (vSize >> 4);
        vSize = ((vSize & 15) << 8) | (buf[6] & 0xFF);

        int pixelAspect = buf[7] & 0xFF;
        int pictFrameRate = pixelAspect & 15;
        pixelAspect >>= 4;

        int bitRate = ((buf[8] & 0xFF) << 8) | (buf[9] & 0xFF);
        int tmp = ((buf[10] & 0xFF) << 8) | (buf[11] & 0xFF);
        bitRate = (bitRate << 2) | (tmp >> 14);
        int vbvBufferSize = (tmp >> 3) & 0x3FF;
        int loadQmatrix = tmp & 3;

        System.out.printf("Sequence hSize=%d vSize=%d aspect=%d frameRate=%d bitRate=%d vbvBufferSize=%d loadQmatrix=%d",
                hSize, vSize, pixelAspect, pictFrameRate, bitRate * 50, vbvBufferSize, loadQmatrix);
    }

    private void decodeExtension() {
        byte[] buf = baos.buf();
        int extensionType = (buf[4] & 0xFF) >> 4;
        System.out.printf("Extension type=%d", extensionType);
    }

    private void decodeGroupOfPictures() {
        byte[] buf = baos.buf();
        int tmp = ((buf[4] & 0xFF) << 24) | ((buf[5] & 0xFF) << 16) | ((buf[6] & 0xFF) << 8) | (buf[7] & 0xFF);
        boolean dropFrameFlag = ((tmp >> 31) & 1) > 0;
        int hh = (tmp >> 26) & 31;
        int mm = (tmp >> 20) & 63;
        // next bit always "1"
        int ss = (tmp >> 13) & 63;
        int frame = (tmp >> 7) & 63;
        boolean closedGOP = ((tmp >> 6) & 1) > 0;
        boolean brokenGOP = ((tmp >> 5) & 1) > 0;
        System.out.printf("GOP drop=%s %02d:%02d:%02d frame=%d", dropFrameFlag, hh, mm, ss, frame);
    }

    private void decodeUserData() {
        System.out.print("UserData");
    }

    private void decodePicture() {
        byte[] buf = baos.buf();
        int tmp = ((buf[4] & 0xFF) << 24) | ((buf[5] & 0xFF) << 16) | ((buf[6] & 0xFF) << 8) | (buf[7] & 0xFF);
        int temporalSequenceNumber = (tmp >> 22) & 0x3FF;
        int frameType = (tmp >> 19) & 7;
        int vbvDelay = (tmp >> 3) & 0xFFFF;
        System.out.printf("Picture tempSeq=%d frameType=%d", temporalSequenceNumber, frameType);
    }

    private void decodeSlice() {
        System.out.printf("Slice");
    }
}
