package ch.oli.web;

import ch.oli.ioctl.DevDvbDemux;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Mpeg2videoDecoder {

    private final DevDvbDemux dmx;
    private final DataOutputStream os;

    public Mpeg2videoDecoder(DevDvbDemux dmx, OutputStream os) {
        this.dmx = dmx;
        this.os = new DataOutputStream(os);
    }

    public void decode() {
        int startCode = 0;
        while (true) {
            // wait for start-code "0x000001??"
            // see http://mugenryodan.blogspot.com/2012/07/mpeg1mpeg2-start-code-table.html
            int countBytes = 0;
            while (true) {
                int b = pull8();
                countBytes++;

                startCode = (startCode << 8) | b;
                if ((startCode & 0xFFFFFF00) == 0x00000100) {
                    System.out.printf("   countBytes=%d\n", countBytes);

                    System.out.printf("%08x: ", startCode);
                    if (startCode >= 0x00000101 && startCode <= 0x000001af) {
                        decodeSlice();
                    } else if (startCode >= 0x000001e0 && startCode <= 0x000001ef) {
                        decodePesVideoStreamHeader();
                    } else {
                        switch (startCode) {
                            case 0x00000100: decodePicture             (); break;
                            case 0x000001b5: decodeExtension           (); break;
                            case 0x000001b2: decodeUserData            (); break;
                            case 0x000001b3: decodeSequence            (); break;
                            case 0x000001b8: decodeGroupOfPictures     (); break;
                        }
                    }
                    countBytes = 0;
                }
            }
        }
    }

    private void decodePesVideoStreamHeader() {
        int pesLen       = pull16(); // should be 0 for video elementary streams
        int pesHeader1   = pull8();
        int pesHeader2   = pull8();
        int pesHeaderLen = pull8();
        System.out.printf("PES Header pesLen=%d pesHeaderLen=%d", pesLen, pesHeaderLen);
        skipBytes(pesHeaderLen);
    }

    private void decodePicture() {
        int tmp = pull32();
        int temporalSequenceNumber = (tmp >> 22) & 0x3FF;
        int frameType = (tmp >> 19) & 7;
        int vbvDelay = (tmp >> 3) & 0xFFFF;
        System.out.printf("Picture tempSeq=%d frameType=%d", temporalSequenceNumber, frameType);
    }

    private void decodeSlice() {
        System.out.print("Slice");
    }

    private void decodeExtension() {
        int extensionType = pull8() >> 4;
        System.out.printf("Extension type=%d", extensionType);
    }

    private void decodeUserData() {
        System.out.print("UserData");
    }

    private void decodeSequence() {
        int hSize = pull8();
        hSize <<= 4;
        int vSize = pull8();
        hSize |= (vSize >> 4);
        vSize = ((vSize & 15) << 8) | pull8();

        int pixelAspect = pull8();
        int pictFrameRate = pixelAspect & 15;
        pixelAspect >>= 4;

        int bitRate = pull16();
        int tmp = pull16();
        bitRate = (bitRate << 2) | (tmp >> 14);
        int vbvBufferSize = (tmp >> 3) & 0x3FF;
        int loadQmatrix = tmp & 3;

        System.out.printf("Sequence hSize=%d vSize=%d aspect=%d frameRate=%d bitRate=%d vbvBufferSize=%d loadQmatrix=%d",
                hSize, vSize, pixelAspect, pictFrameRate, bitRate * 50, vbvBufferSize, loadQmatrix);
    }

    private void decodeGroupOfPictures() {
        int tmp = pull32();
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


    private int pull8() {
        try {
            int v = dmx.file.readUnsignedByte();
            os.writeByte(v);
            return v;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private int pull16() {
        try {
            int v = dmx.file.readUnsignedShort();
            os.writeShort(v);
            return v;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private int pull32() {
        try {
            int v = dmx.file.readInt();
            os.writeInt(v);
            return v;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private byte[] skipBuffer = new byte[1024];
    private void skipBytes(int num) {
        try {
            while (num > 0) {
                int read = dmx.file.read(skipBuffer, 0, Math.min(skipBuffer.length, num));
                os.write(skipBuffer, 0, read);
                num -= read;
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
