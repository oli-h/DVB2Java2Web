package ch.oli;

import java.io.ByteArrayOutputStream;

public class MyBAOS extends ByteArrayOutputStream {

    public PacketReader asPacketReader() {
        return new PacketReader(buf, 0, count);
    }
}
