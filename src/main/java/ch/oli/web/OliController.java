package ch.oli.web;

import ch.oli.decode.PacketReader;
import ch.oli.ioctl.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class OliController {

    private DevDvbFrontend fe;

    @Autowired
    private DecoderPAT decoderPAT;

    @Autowired
    private DecoderPMT decoderPMT;

    @Autowired
    private DecoderNIT decoderNIT;

    @Autowired
    private DecoderSDT decoderSDT;

    @Autowired
    private DecoderEIT decoderEIT;

//    @PostConstruct
//    public void postConstruct() {
//    }

    public static class TuneParams {
        public int frequency;
        public int symbol_rate;
        public C.fe_modulation modulation;
    }

    @PostMapping(value = "/tune", produces = MediaType.TEXT_PLAIN_VALUE)
    public String tune(@RequestBody TuneParams tuneParams) throws Exception {
        stopAllRunningPidReceivers();
        stopFrontend();
        fe = new DevDvbFrontend(1);

        long t0 = System.currentTimeMillis();

        C.fe_delivery_system       delSys    = C.fe_delivery_system      .SYS_DVBC_ANNEX_A    ;
        C.dvbfe_spectral_inversion inversion = C.dvbfe_spectral_inversion.DVBFE_INVERSION_AUTO;
        C.dvbfe_code_rate          fec       = C.dvbfe_code_rate         .DVBFE_FEC_NONE      ;

        dtv_properties dtvprops = new dtv_properties();
        dtvprops.addCmd(C.CMD.DTV_DELIVERY_SYSTEM, delSys               .ordinal());
        dtvprops.addCmd(C.CMD.DTV_FREQUENCY      , tuneParams.frequency           );
        dtvprops.addCmd(C.CMD.DTV_SYMBOL_RATE    , tuneParams.symbol_rate         );
        dtvprops.addCmd(C.CMD.DTV_MODULATION     , tuneParams.modulation.ordinal());
        dtvprops.addCmd(C.CMD.DTV_INVERSION      , inversion            .ordinal());
        dtvprops.addCmd(C.CMD.DTV_INNER_FEC      , fec                  .ordinal());
        dtvprops.addCmd(C.CMD.DTV_TUNE           , 0);
        fe.feSetProperty(dtvprops);

        while (true) {
            long millis = System.currentTimeMillis() - t0;
            int status = fe.feReadStatus();
            if (status == 31) {
                startPidReceiverIfNotYetStarted(0x00); // PAT
                startPidReceiverIfNotYetStarted(0x10); // NIT
                startPidReceiverIfNotYetStarted(0x11); // SDT
//                startPidReceiverIfNotYetStarted(0x12); // EIT
                return "LOCK in " + millis + " ms";
            }
            if (millis >= 500) {
                return "NO LOCK - giving up after " + millis + " ms";
            }
            Thread.sleep(10);
        }
    }

    @PostMapping(value = "/stopFrontend")
    public void stopFrontend() {
        if (fe != null) {
            fe.close();
            fe = null;
        }
    }

    @GetMapping(value = "/pes/{pid}")
    public void stream(@PathVariable int pid, HttpServletResponse resp) {
        try (DevDvbDemux dmx = fe.openDedmux()) {
            dmx.dmxSetBufferSize(8192);

            dmx_pes_filter_params filter = new dmx_pes_filter_params();
            filter.pid = (short) pid;
            filter.input = dmx_pes_filter_params.dmx_input.DMX_IN_FRONTEND;
//            filter.output = dmx_pes_filter_params.dmx_output.DMX_OUT_TAP;
            filter.output = dmx_pes_filter_params.dmx_output.DMX_OUT_TSDEMUX_TAP;
            filter.pes_type = dmx_pes_filter_params.dmx_ts_pes.DMX_PES_OTHER;
            filter.flags = dmx_sct_filter_params.DMX_IMMEDIATE_START;
            dmx.dmxSetPesFilter(filter);

            resp.setContentType("audio/mp2t");
            byte[] buf = new byte[8192];
            long max = System.currentTimeMillis() + 10_000_000;
            while (System.currentTimeMillis() < max) {
                int read = dmx.file.read(buf);
                System.out.println("read " + read + " bytes");
                resp.getOutputStream().write(buf, 0, read);
                System.out.println("wrote " + read + " bytes");
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private ConcurrentHashMap<Integer, PidReceiver> pidReceivers = new ConcurrentHashMap<>();

    private void stopAllRunningPidReceivers() {
        while (pidReceivers.size() > 0) {
            Integer pid = pidReceivers.keys().nextElement();
            pidReceivers.remove(pid).close();
        }
    }

    public void startPidReceiverIfNotYetStarted(int pid) {
        pidReceivers.computeIfAbsent(pid, newPid -> {
            PidReceiver pr = new PidReceiver(newPid);
            pr.start();
            return pr;
        });
    }

    public class PidReceiver extends Thread {

        private final DevDvbDemux dmx;

        public PidReceiver(int pid) {
            dmx = fe.openDedmux();

            dmx.dmxSetBufferSize(8192);

            dmx_sct_filter_params dmxSct = new dmx_sct_filter_params();
            dmxSct.pid = (short) pid;
            dmxSct.flags = dmx_sct_filter_params.DMX_CHECK_CRC | dmx_sct_filter_params.DMX_IMMEDIATE_START;
            dmxSct.timeout = 1000;
            dmx.dmxSetFilter(dmxSct);
        }

        @Override
        public void run() {
            byte[] buf = new byte[4096];
            try {
                while (true) {
                    int read = dmx.file.read(buf);
                    PacketReader prPU = new PacketReader(buf, 0, read);
                    decodePSI(prPU);
//                    System.out.println();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        public void close() {
            dmx.close();
        }
    }


    /**
     * PSI = "Program Specific Information"
     * Transferred in PID 0 (PAT), PID 16 (NIT, ST), PID 17 (SDT, BAT, ST), PID 18 (EIT, ST, CIT)
     * and some others
     */
    public void decodePSI(PacketReader prPU) {
        // https://www.etsi.org/deliver/etsi_en/300400_300499/300468/01.16.01_60/en_300468v011601p.pdf

        while (prPU.hasBytes()) {
            int table_id = prPU.pull8();
            if (table_id == 0xFF) {
                break;
            }
            int tmp = prPU.pull16();
            int section_length = tmp & 0xFFF; // 12 Bit
            if (section_length > 4093) {
                System.out.format("ERROR: sectLen %d larger than allowed 4093. Ignoring rest of packet unit", section_length);
                break;
            }
            PacketReader prSection = prPU.nextBytesAsPR(section_length);
            int something = prSection.pull16(); // semantic depends on table_id - still we need to decode it here
            tmp = prSection.pull8();
            int version_number = (tmp >> 1) & 0x1F;
            int current_next_indicator = tmp & 1; // 1=current 0=next
            int section_number = prSection.pull8();
            int last_section_number = prSection.pull8();

            // in PID 0: Program Association Table (PAT)
            if (table_id == 0x00) {
                decoderPAT.decode(prSection, something, this);
            }
            // in PID 1: Conditional Access Table (CAT)
            else if (table_id == 0x01) { // conditional_access_section"
//                decodeCAT(prSection, table_id, something);
            }
            // in PID 16: Network Information Table (NIT)
            else if (table_id == 0x40 || table_id == 0x41) { // actual_network (0x40) or other_network 0x41
                decoderNIT.decode(prSection, something);
            }
            // in PID 17: Service Description Table (SDT)
            else if (table_id == 0x42 || table_id == 0x46) { // actual_transport_stream (0x42) or other_transport_stream (0x46)
                decoderSDT.decode(prSection, something);
                // in PID 18: Event Information Table (EIT)
            } else if (table_id >= 0x4E && table_id <= 0x6F) {
                decoderEIT.decode(prSection, something);
            }
            // in some PID: Program Map Table (PMT)
            else if (table_id == 0x02) { // program_map_section
                decoderPMT.decode(prSection, something);
            }
        }
    }
}



