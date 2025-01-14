package ch.oli.dvb;

import ch.oli.ioctl.C;
import ch.oli.ioctl.DevDvbDemux;
import ch.oli.ioctl.dmx_pes_filter_params;
import ch.oli.ioctl.dmx_sct_filter_params;
import ch.oli.web.OliController;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class DvbTests {


    @Test
    @Disabled
    public void dvbFramesToUDP() throws Exception {
        final int adapter = 1;
        OliController oc = new OliController();
        var tuneParams = new OliController.TuneParams();
        tuneParams.frequency = 322_000_000;
        tuneParams.modulation= C.fe_modulation.QAM_256;
        tuneParams.symbol_rate = 6_900_000;

        String resp = oc.tune(tuneParams, adapter);
        System.out.println(resp);

        final InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 5555);
        final DatagramSocket udpSocket = new DatagramSocket(5555);

        try (DevDvbDemux dmx = oc.fe[adapter].openDemux()) {
            dmx.dmxSetBufferSize(256 * 1024);

            dmx_pes_filter_params filter = new dmx_pes_filter_params();
            filter.pid = (short) 0x2000;
            filter.input = dmx_pes_filter_params.dmx_input.DMX_IN_FRONTEND;
            filter.output = dmx_pes_filter_params.dmx_output.DMX_OUT_TSDEMUX_TAP;
            filter.pes_type = dmx_pes_filter_params.dmx_ts_pes.DMX_PES_OTHER;
            filter.flags = dmx_sct_filter_params.DMX_IMMEDIATE_START;
            dmx.dmxSetPesFilter(filter);

            byte[] buf = new byte[188 * 5];
            while (true) {
                int read = dmx.file.read(buf);
                if ((read % 188) != 0) {
                    throw new RuntimeException("Upps - not multiple of 188");
                }
                for (int i = 0; i < read; i += 188) {
                    if (buf[i] != 0x47) {
                        throw new RuntimeException("Upps - no sync-byte 0x47 at packed start");
                    }
                    DatagramPacket udp = new DatagramPacket(buf, i, 188, addr);
                    udpSocket.send(udp);
                }
            }
        }

    }

    @Test
    @Disabled
    public void signalAndBitRates() throws Exception {
        OliController oc = new OliController();
        var tuneParams = new OliController.TuneParams();
        tuneParams.frequency = 122_000_000;
        tuneParams.modulation= C.fe_modulation.QAM_256;
        tuneParams.symbol_rate = 6_900_000;

        String resp = oc.tune(tuneParams, 1);
        System.out.println(resp);

        Average postTotal = new Average();
        Average postError = new Average();
        Average errorBlock = new Average();
        long nextTick = System.currentTimeMillis() + 1000;
        while (true) {
            nextTick += 1000;
            Thread.sleep(nextTick - System.currentTimeMillis());

            OliController.TuneStats ts = oc.tuneStats(1);
            postTotal.update(ts.postTotalBitCount);
            postError.update(ts.postErrorBitCount);
            errorBlock.update(ts.errorBlockCount);

            System.out.printf("SNR=%.2f Signal=%.2f postTotal=%.3f MBit/s postError=%.2f Bit/s errorBlock=%.2f %sCARRIER%s %sLOCK%s %sSIGNAL%s %sINNER_STABLE%s %sSYNC%s\n",
                    ts.signalNoiceRatio_dBm,
                    ts.signalStrength_dBm,
                    postTotal.avg / 1_000_000.0,
                    postError.avg,
                    errorBlock.avg,
                    ts.statusHasCarrier         ? ANSI.GREEN : ANSI.RED, ANSI.RESET,
                    ts.statusHasLock            ? ANSI.GREEN : ANSI.RED, ANSI.RESET,
                    ts.statusHasSignal          ? ANSI.GREEN : ANSI.RED, ANSI.RESET,
                    ts.statusHasInnerCodeStable ? ANSI.GREEN : ANSI.RED, ANSI.RESET,
                    ts.statusHasSync            ? ANSI.GREEN : ANSI.RED, ANSI.RESET
            );
        }
    }

    public static class Average {
        private long prev = 0;
        public double avg = 0;

        public void update(long value) {
            if (prev > 0) {
                long diff = value - prev;
                if (avg == 0) {
                    avg = diff;
                }
                avg = 0.99 * avg + 0.01 * diff;
            }
            prev = value;
        }
    }
}
