package ch.oli.web;

import ch.oli.decode.PacketReader;
import ch.oli.ioctl.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedDeque;

@RestController
public class OliController {

    private DevDvbFrontend fe;

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
                startPidReceiver(0x10); // NIT
                startPidReceiver(0x11); // SDT
                startPidReceiver(0x12); // EIT
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

    private ConcurrentLinkedDeque<PidReceiver> pidReceivers = new ConcurrentLinkedDeque<>();

    private void stopAllRunningPidReceivers() {
        while (true) {
            PidReceiver pr = pidReceivers.poll();
            if (pr == null) {
                return;
            }
            pr.close();
        }
    }

    private void startPidReceiver(int pid) {
        PidReceiver pr = new PidReceiver(pid);
        pidReceivers.add(pr);
        pr.start();
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

//                System.out.format("(TableID %d sectLen=%4d Section %d/%d)", table_id, section_length, section_number, last_section_number);

            // in PID 0: Program Association Table (PAT)
            if (table_id == 0x00) {
                decodePAT(prSection, table_id ,something);
            }
            // in PID 1: Conditional Access Table (CAT)
            else if (table_id == 0x01) { // conditional_access_section"
//                decodeCAT(prSection, table_id, something);
            }
            // in PID 16: Network Information Table (NIT)
            else if (table_id == 0x40 || table_id == 0x41) { // actual_network (0x40) or other_network 0x41
                decoderNIT.decode(prSection, table_id, something);
            }
            // in PID 17: Service Description Table (SDT)
            else if (table_id == 0x42 || table_id == 0x46) { // actual_transport_stream (0x42) or other_transport_stream (0x46)
                decoderSDT.decode(prSection, table_id, something);
            // in PID 18: Event Information Table (EIT)
            } else if(table_id >=0x4E && table_id<=0x6F) {
                decoderEIT.decode(prSection, table_id, something);
            }
            // in some PID: Program Map Table (PMT)
            else if (table_id == 0x02) { // program_map_section
//                decodePMT(prSection, table_id, something);
            }

        }
    }

    private void decodePAT(PacketReader prSection, int table_id, int something) {
//        System.out.printf(" PAT:");
        while (prSection.remain() > 4) {
            int program = prSection.pull16();
            int pmtPID = prSection.pull16() & 0x1FFF;
//            System.out.format(" Prog:%d->PMT-PID:%d", program, pmtPID);
        }
    }

}



