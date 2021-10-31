package ch.oli.ioctl;

public class dvb_frontend_parameters extends IoctlBase {
    public dvb_frontend_parameters() {
        super(36);
    }

    enum dvbfe_spectral_inversion {
        DVBFE_INVERSION_OFF,
        DVBFE_INVERSION_ON,
        DVBFE_INVERSION_AUTO
    }

    enum dvbfe_code_rate {
        DVBFE_FEC_NONE,
        DVBFE_FEC_1_2,
        DVBFE_FEC_2_3,
        DVBFE_FEC_3_4,
        DVBFE_FEC_4_5,
        DVBFE_FEC_5_6,
        DVBFE_FEC_6_7,
        DVBFE_FEC_7_8,
        DVBFE_FEC_8_9,
        DVBFE_FEC_AUTO
    }

    enum fe_modulation {
        QPSK,
        QAM_16,
        QAM_32,
        QAM_64,
        QAM_128,
        QAM_256,
        QAM_AUTO,
        VSB_8,
        VSB_16,
        PSK_8,
        APSK_16,
        APSK_32,
        DQPSK,
        QAM_4_NR
    }

    public int                      frequency  ;
    public dvbfe_spectral_inversion inversion  ;
    // following params are _QAM_only_ (in original C-Code this is a union)
    public int                      symbol_rate;
    public dvbfe_code_rate          fec_inner  ;
    public fe_modulation            modulation ;

    public void setViaIoctl(int fdFrontend) {
        int32( 0, frequency            );
        int32( 4, inversion  .ordinal());
        int32( 8, symbol_rate          );
        int32(12, fec_inner  .ordinal());
        int32(16, modulation .ordinal());

        final int FE_SET_FRONTEND = 76;
        doIoctl(fdFrontend, DIR.write, FE_SET_FRONTEND);
    }

//    public void readViaIoctl(int fdFrontend) {
//    }

}
