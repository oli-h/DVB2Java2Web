package ch.oli.decode;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class PacketReader {

    public final byte[] buf;
    public final int initialOffset;
    public final int initialLength;
    private int idx;
    private int remain;

    public PacketReader(byte[] buf, int offset, int length) {
        this.buf = buf;
        this.initialOffset = offset;
        this.initialLength = length;
        this.idx = offset;
        this.remain = length;
    }

    public byte[] wholePacket() {
        return Arrays.copyOfRange(buf, initialOffset, initialOffset + initialLength);
    }

    public int pull8() {
        if (remain < 1) {
            throw new RuntimeException("not enough bytes");
        }
        remain--;
        idx++;
        return buf[idx - 1] & 0xFF;
    }

    public int pull16() {
        if (remain < 2) {
            throw new RuntimeException("not enough bytes");
        }
        remain -= 2;
        idx += 2;
        return ((buf[idx - 2] & 0xFF) << 8) | (buf[idx - 1] & 0xFF);
    }

    public int pull24() {
        if (remain < 3) {
            throw new RuntimeException("not enough bytes");
        }
        remain -= 3;
        idx += 3;
        return ((buf[idx - 3] & 0xFF) << 16) |((buf[idx - 2] & 0xFF) << 8) | (buf[idx - 1] & 0xFF);
    }

    public static long fromBCD(long bcd) {
        long value = 0;
        long mult = 1;
        while (bcd != 0) {
            value += (bcd & 15) * mult;
            mult *= 10;
            bcd >>= 4;
        }
        return value;
    }

    public static int fromBCD(int bcd) {
        int value = 0;
        int mult = 1;
        while (bcd != 0) {
            value += (bcd & 15) * mult;
            mult *= 10;
            bcd >>= 4;
        }
        return value;
    }

    public long pull32() {
        if (remain < 4) {
            throw new RuntimeException("not enough bytes");
        }
        remain -= 4;
        idx += 4;
        return ((buf[idx - 4] & 0xFFL) << 24) | ((buf[idx - 3] & 0xFFL) << 16) | ((buf[idx - 2] & 0xFFL) << 8) | (buf[idx - 1] & 0xFFL);
    }

    public String pullChar(int length) {
        if (remain < length) {
            throw new RuntimeException("not enough bytes");
        }
        remain -= length;
        idx += length;

        Charset charset = StandardCharsets.ISO_8859_1; // not correct - should be ISO 6937
        if (buf[idx - length] < 0x20) {
            length--;
            switch (buf[idx - length]) {
                case 1: charset = Charset.forName("ISO-8859-5"); break;
                case 2: charset = Charset.forName("ISO-8859-6"); break;
                case 3: charset = Charset.forName("ISO-8859-7"); break;
                case 4: charset = Charset.forName("ISO-8859-8"); break;
                case 5: charset = Charset.forName("ISO-8859-9"); break;
            }
        }
        return new String(buf, idx - length, length, charset);
    }

    public void pull(int length, byte[] dest, int destPos) {
        if (remain < length) {
            throw new RuntimeException("not enough bytes");
        }
        System.arraycopy(buf, idx, dest, destPos, length);
        remain -= length;
        idx += length;
    }

    public void skip(int length) {
        if (remain < length) {
            throw new RuntimeException("not enough bytes");
        }
        remain -= length;
        idx += length;
    }

    public boolean hasBytes() {
        return remain > 0;
    }
    public int remain() {
        return remain;
    }

    public void writeRemainTo(OutputStream os) {
        if (remain < 1) {
            return;
        }
        try {
            os.write(buf, idx, remain);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        idx += remain;
        remain = 0;
    }

    public PacketReader nextBytesAsPR(int length) {
        if (remain < length) {
            throw new RuntimeException("not enough bytes");
        }
        remain -= length;
        idx += length;
        return new PacketReader(buf, idx - length, length);
    }

}
