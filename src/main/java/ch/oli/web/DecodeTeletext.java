package ch.oli.web;

import ch.oli.decode.MyBAOS;
import ch.oli.decode.PacketReader;
import ch.oli.ioctl.DevDvbDemux;

import java.io.OutputStream;

public class DecodeTeletext {

    public void decode(DevDvbDemux dmx, OutputStream os) throws Exception {
        MyBAOS baos = new MyBAOS();
        byte[] buf = new byte[1024];
        byte[] pck = new byte[46]; // size is 45. But specification starts to count byte-numbers with "1"
        while (true) {
            // wait for PES start code 00 00 01
            while (true) {
                while (dmx.file.readByte() != 0);
                if (dmx.file.readByte() == 0) {
                    if (dmx.file.readByte() == 1) {
                        break;
                    }
                }
            }
            int streamId = dmx.file.readByte() & 0xFF; // shall be set to "1011 1101" meaning "private_stream_1"
            int pesPacketLen = dmx.file.readShort() & 0xFFFF; // should be (N × 184)-6
            baos.reset();
            for (int remain = pesPacketLen; remain > 0; ) {
                int read = dmx.file.read(buf, 0, Math.min(buf.length, remain));
                baos.write(buf, 0, read);
                remain -= read;
            }
            PacketReader pr = baos.asPacketReader();
//                System.out.println("read " + pr.initialLength + " bytes");
//                PacketDecoderDVB.hexDump(pr.wholePacket());
            pr.skip(45 - 6); // skip Header as defined in ETSI EN 300 472 V1.4.1 (2017-04)

            int data_identifier = pr.pull8();  // should be 0x10 to 0x1F="EBU data" - all other values are reserved or user defined
            while (pr.remain() >= 2) {
                // 0x02=EBU Teletext non-subtitle data
                // 0x03=EBU Teletext subtitle data
                // 0xFF=stuffing
                // all other values are reserved or user defined
                int data_unit_id = pr.pull8();
                int data_unit_length = pr.pull8(); // should always be 0x2c = 44 decimal
                if (data_unit_length != 44) {
                    System.out.println("data_unit_length not 44");
                    continue;
                }
                pr.skip(1); // skip field_parity and line_offset
                // the remaining 43 bytes are defined in ets_300706e01p.pdf (coming from Analog-TV)
                pr.pull(43, pck,3); // bytes 1 and 2 ('Clock run-in') are not present in DVB

                if (data_unit_id == 0xFF) {
                    // stuffing --> skip
                    continue;
                }

                int framing_code = pck[3] & 0xFF;    // should be binary 11100100 (=0xe4)
                if (framing_code != 0xe4) {
                    System.out.println("framing_code not 0xe4");
                    continue;
                }
                int mpag = bytereverse((hamming_8_4(pck[4]) << 4) | hamming_8_4(pck[5]));
                int magazine = (mpag & 7) == 0 ? 8 : (mpag & 7);
                int row = (mpag >> 3) & 31;
                if (row == 0) {
                    pageHeaderPacket(magazine, pck);
                } else if (row <= 25) {
                    normalPacket(magazine, row, pck);
                } else {
                    // nonDisplayablePacket();
                }
            }

            os.write(pr.buf, pr.initialOffset, pr.initialLength);
            os.flush();
//                System.out.println("wrote " + pr.initialLength + " bytes");
        }
    }

    private void pageHeaderPacket(int magazine, byte[] pck) {
        int pageNumber = bytereverse((hamming_8_4(pck[6]) << 4) | hamming_8_4(pck[7]));
        int s1 = bytereverse(hamming_8_4(pck[ 8])) & 0xF;
        int s2 = bytereverse(hamming_8_4(pck[ 9])) & 0x7;
        int s3 = bytereverse(hamming_8_4(pck[10])) & 0xF;
        int s4 = bytereverse(hamming_8_4(pck[11])) & 0x3;

        string(pck, 14, 32);
//        System.out.println("-----------------------------------------------------");
        System.out.format("Mag/Page %d%02X %s (SubCode %x%x%x%x)\n", magazine, pageNumber, sb, s1, s2, s3, s4);
    }

    private final StringBuilder sb = new StringBuilder(40);
    private void normalPacket(int magazine, int row, byte[] pck) {
        string(pck,6,40);
//        if (row <= 23) {
        System.out.format("Mag %d Row %2d %s\n", magazine, row, sb);
//        }
    }

    private void string(byte[] pck, int idx0, int len) {
        sb.setLength(0);
        for (int i = 0; i < len; i++) {
            int b = bytereverse(pck[idx0 + i]) & 0x7f; // ignore parity
            switch (b) {
                case 0x23: b = '#'; break;
                case 0x24: b = '$'; break;
                case 0x40: b = '§'; break;
                case 0x5b: b = 'Ä'; break;
                case 0x5c: b = 'Ö'; break;
                case 0x5d: b = 'Ü'; break;
                case 0x5e: b = '^'; break;
                case 0x5f: b = '_'; break;
                case 0x7b: b = 'ä'; break;
                case 0x7c: b = 'ö'; break;
                case 0x7d: b = 'ü'; break;
                case 0x7e: b = 'ß'; break;
            }
            System.out.print(" " + b);
            sb.append((char) b);
        }
    }

    private int decodeHamming8_4(int input) {
        int decoded = 0;
        int shift = 1;
        while (input > 0) {
            if ((input & 1) > 0) {
                decoded |= shift;
            }
            shift <<= 1;
            input >>= 2;
        }
        return decoded;
    }

    public int bytereverse(int n) {
        n = (((n >> 1) & 0x55) | ((n << 1) & 0xaa));
        n = (((n >> 2) & 0x33) | ((n << 2) & 0xcc));
        n = (((n >> 4) & 0x0f) | ((n << 4) & 0xf0));
        return n;
    }

    public int hamming_8_4(int a) {
        switch (a & 0xFF) {
            case 0xA8: return  0;
            case 0x0B: return  1;
            case 0x26: return  2;
            case 0x85: return  3;
            case 0x92: return  4;
            case 0x31: return  5;
            case 0x1C: return  6;
            case 0xBF: return  7;
            case 0x40: return  8;
            case 0xE3: return  9;
            case 0xCE: return 10;
            case 0x6D: return 11;
            case 0x7A: return 12;
            case 0xD9: return 13;
            case 0xF4: return 14;
            case 0x57: return 15;
            default  : return -1;     // decoding error , not yet corrected
        }
    }

}
