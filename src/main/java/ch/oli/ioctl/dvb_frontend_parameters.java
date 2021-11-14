package ch.oli.ioctl;

import com.sun.jna.Structure;

@Structure.FieldOrder({"frequency","inversion","symbol_rate","fec_inner","modulation","filler_for_union"})
public class dvb_frontend_parameters extends Structure {
    public int                        frequency  ;
    public C.dvbfe_spectral_inversion inversion  ;
    // following params are _QAM_only_ (in original C-Code this is a union)
    public int                        symbol_rate;
    public C.dvbfe_code_rate          fec_inner  ;
    public C.fe_modulation            modulation ;
    public int[] filler_for_union = new int[4];

    public void setViaIoctl(int fdFrontend) {
        final int FE_SET_FRONTEND = 76;
        C.ioctl(fdFrontend, C.DIR.write, FE_SET_FRONTEND, this);
    }
}
