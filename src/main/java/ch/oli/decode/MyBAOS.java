package ch.oli.decode;

import java.io.ByteArrayOutputStream;

public class MyBAOS extends ByteArrayOutputStream {

    public PacketReader asPacketReader() {
        return new PacketReader(buf, 0, count);
    }

    public void removeBytesFromTail(int numBytes) {
        if (count > numBytes) {
            count -= numBytes;
        } else {
            count = 0;
        }
    }

    public byte[] buf() {
        return buf;
    }
}
