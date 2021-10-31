package ch.oli.ioctl;

public class dmx_pes_filter_params extends IoctlBase {
    protected dmx_pes_filter_params() {
        super(20);
    }

    /**
     * enum dmx_input - Input from the demux.
     *
     * @DMX_IN_FRONTEND: Input from a front-end device.
     * @DMX_IN_DVR: Input from the logical DVR device.
     */
    enum dmx_input {
        DMX_IN_FRONTEND,
        DMX_IN_DVR
    }

    ;


    /**
     * enum dmx_output - Output for the demux.
     *
     * @DMX_OUT_DECODER: Streaming directly to decoder.
     * @DMX_OUT_TAP: Output going to a memory buffer (to be retrieved via the read command).
     * Delivers the stream output to the demux device on which the ioctl
     * is called.
     * @DMX_OUT_TS_TAP: Output multiplexed into a new TS (to be retrieved by reading from the
     * logical DVR device). Routes output to the logical DVR device
     * ``/dev/dvb/adapter?/dvr?``, which delivers a TS multiplexed from all
     * filters for which @DMX_OUT_TS_TAP was specified.
     * @DMX_OUT_TSDEMUX_TAP: Like @DMX_OUT_TS_TAP but retrieved from the DMX device.
     */
    enum dmx_output {
        DMX_OUT_DECODER,
        DMX_OUT_TAP,
        DMX_OUT_TS_TAP,
        DMX_OUT_TSDEMUX_TAP
    }

    /**
     * enum dmx_ts_pes - type of the PES filter.
     *
     * @DMX_PES_AUDIO0:    first audio PID. Also referred as @DMX_PES_AUDIO.
     * @DMX_PES_VIDEO0:    first video PID. Also referred as @DMX_PES_VIDEO.
     * @DMX_PES_TELETEXT0: first teletext PID. Also referred as @DMX_PES_TELETEXT.
     * @DMX_PES_SUBTITLE0: first subtitle PID. Also referred as @DMX_PES_SUBTITLE.
     * @DMX_PES_PCR0:      first Program Clock Reference PID.
     * Also referred as @DMX_PES_PCR.
     * @DMX_PES_AUDIO1:    second audio PID.
     * @DMX_PES_VIDEO1:    second video PID.
     * @DMX_PES_TELETEXT1: second teletext PID.
     * @DMX_PES_SUBTITLE1: second subtitle PID.
     * @DMX_PES_PCR1:      second Program Clock Reference PID.
     * @DMX_PES_AUDIO2:    third audio PID.
     * @DMX_PES_VIDEO2:    third video PID.
     * @DMX_PES_TELETEXT2: third teletext PID.
     * @DMX_PES_SUBTITLE2: third subtitle PID.
     * @DMX_PES_PCR2:      third Program Clock Reference PID.
     * @DMX_PES_AUDIO3:    fourth audio PID.
     * @DMX_PES_VIDEO3:    fourth video PID.
     * @DMX_PES_TELETEXT3: fourth teletext PID.
     * @DMX_PES_SUBTITLE3: fourth subtitle PID.
     * @DMX_PES_PCR3:      fourth Program Clock Reference PID.
     * @DMX_PES_OTHER:     any other PID.
     */

    enum dmx_ts_pes {
        DMX_PES_AUDIO0,
        DMX_PES_VIDEO0,
        DMX_PES_TELETEXT0,
        DMX_PES_SUBTITLE0,
        DMX_PES_PCR0,

        DMX_PES_AUDIO1,
        DMX_PES_VIDEO1,
        DMX_PES_TELETEXT1,
        DMX_PES_SUBTITLE1,
        DMX_PES_PCR1,

        DMX_PES_AUDIO2,
        DMX_PES_VIDEO2,
        DMX_PES_TELETEXT2,
        DMX_PES_SUBTITLE2,
        DMX_PES_PCR2,

        DMX_PES_AUDIO3,
        DMX_PES_VIDEO3,
        DMX_PES_TELETEXT3,
        DMX_PES_SUBTITLE3,
        DMX_PES_PCR3,

        DMX_PES_OTHER
    }

    public short pid;
    public dmx_input  input;
    public dmx_output output;
    public dmx_ts_pes pes_type;
    public int flags;

    public void setViaIoctl(int fdDemux) {
        int16( 0, pid               );
        int32( 4, input.ordinal()   );
        int32( 8, output.ordinal()  );
        int32(12, pes_type.ordinal());
        int32(16, flags             );

        final int DMX_SET_PES_FILTER = 44;
        doIoctl(fdDemux, DIR.write, DMX_SET_PES_FILTER);
    }

}
