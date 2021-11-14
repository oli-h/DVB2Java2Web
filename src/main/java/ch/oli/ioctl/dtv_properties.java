package ch.oli.ioctl;

import com.sun.jna.Structure;
import com.sun.jna.Union;

@Structure.FieldOrder({"num", "props"})
public class dtv_properties extends Structure {
    // this array finall is sequential in native memory
    // private = not mapped. Mapping is done via next property "props"
    private dtv_property[] propsArray = (dtv_property[]) new dtv_property().toArray(16);

    public int          num   = 0            ;
    public dtv_property props = propsArray[0]; // native only needs pointer to first element of array

    // sizeof = 76
    @Structure.FieldOrder({"cmd", "reserved1", "u", "result"})
    public static class dtv_property extends Structure implements ByReference {

        public dtv_property() {
            super(ALIGN_NONE); // ALIGN_NONE = __packed (in C-structs)
        }

        public int cmd;
        public int[] reserved1 = new int[3];
        public PropUnion u;
        public int result;

        public static class PropUnion extends Union {
            public int data;
            public dtv_fe_stats st;
            public Buffer buffer;
        }
        @FieldOrder({"len", "stat"})
        public static class dtv_fe_stats extends Structure {
            public byte len;
            public dtv_stats[] stat = new dtv_stats[4];
        }
        @FieldOrder({"scale", "value"})
        public static class dtv_stats extends Structure {
            public dtv_stats() {
                super(ALIGN_NONE); // ALIGN_NONE = __packed (in C-structs)
            }
            public byte scale;	// enum fecap_scale_params type
            public long value;  // either uvalue or svalue - originally it is a "union"

            enum fecap_scale_params {
                FE_SCALE_NOT_AVAILABLE,
                FE_SCALE_DECIBEL      ,
                FE_SCALE_RELATIVE     ,
                FE_SCALE_COUNTER
            }
        }
        @FieldOrder({"data", "len", "reserved1", "reserved2"})
        public static class Buffer extends Structure {
            public final byte[] data = new byte[32];
            public int len;
            public final int[] reserved1 = new int[3];
            public String reserved2;
        }
    }

    /**
     * using the "data field" in the union
     */
    public void addCmd(C.CMD cmd, int data) {
        dtv_property prop = propsArray[num++];
        prop.cmd = cmd.ordinal();
        prop.u.setType("data");
        prop.u.data = data;
    }

    /**
     * using the "buffer field" in the union
     */
    public void addCmd(C.CMD cmd, byte[] buffer) {
        dtv_property prop = propsArray[num++];
        prop.cmd = cmd.ordinal();
        prop.u.setType("buffer");
        prop.u.buffer = new dtv_property.Buffer();
        prop.u.buffer.len = buffer.length;
        System.arraycopy(buffer, 0, prop.u.buffer.data, 0, buffer.length);
    }

    /**
     * using the "st field" in the union
     */
    public void addStatsCmd(C.CMD cmd) {
        dtv_property prop = propsArray[num++];
        prop.cmd = cmd.ordinal();
        prop.u.setType("st");
        prop.u.st = new dtv_property.dtv_fe_stats();
    }

    public void clear() {
        for (int i = 0; i < propsArray.length; i++) {
            dtv_property prop = propsArray[i];
            prop.cmd = 0;
            prop.u.setType("data");
            prop.u.data = 0;
            prop.u.buffer = null;
            prop.u.st = null;
        }
        num = 0;
    }

    public dtv_property[] getPropsArray() {
        return propsArray;
    }
}
