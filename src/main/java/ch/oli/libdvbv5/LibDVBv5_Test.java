package ch.oli.libdvbv5;

public class LibDVBv5_Test {
    public static void main(String[] args) {
//        System.out.println(new LibDVBv5.dvb_v5_fe_parms() );
        LibDVBv5.dvb_v5_fe_parms o = LibDVBv5.INSTANCE.dvb_fe_open(0, 0, 0, 0);
        System.out.println(o);
    }
}
