package ch.oli.web;

import ch.oli.decode.PacketReader;
import ch.oli.ioctl.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class OliController {

    public DevDvbFrontend[] fe = new DevDvbFrontend[16];

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

    public static class TuneParams {
        public int frequency;
        public int symbol_rate;
        public C.fe_modulation modulation;
    }

    @PostMapping(value = "/tune", produces = MediaType.TEXT_PLAIN_VALUE)
    public String tune(@RequestBody TuneParams tuneParams, @RequestParam(defaultValue = "1") int adapter) throws Exception {
//        stopAllRunningPidReceivers();
        stopFrontend(adapter);
        fe[adapter] = new DevDvbFrontend(adapter);

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
        fe[adapter].feSetProperty(dtvprops);

        while (true) {
            long millis = System.currentTimeMillis() - t0;
            int status = fe[adapter].feReadStatus();
            if (status == 31) {
                return "LOCKED in " + millis + " ms";
            }
            if (millis >= 1000) {
                stopFrontend(adapter);
                return "no lock (" + millis + " ms)";
            }
            Thread.sleep(10);
        }
    }

    @GetMapping("/tune")
    public TuneParams tune(@RequestParam(defaultValue = "1") int adapter) {
        TuneParams tuneParams = new TuneParams();
        dtv_properties dtvprops = new dtv_properties();
        dtvprops.addStatsCmd(C.CMD.DTV_FREQUENCY           ); // Index 0
        dtvprops.addStatsCmd(C.CMD.DTV_SYMBOL_RATE         ); // Index 1
        dtvprops.addStatsCmd(C.CMD.DTV_MODULATION          ); // Index 2
        fe[adapter].feGetProperty(dtvprops);
        tuneParams.frequency = dtvprops.getPropsArray()[0].u.data;
        tuneParams.symbol_rate = dtvprops.getPropsArray()[1].u.data;
        tuneParams.modulation = C.fe_modulation.values()[dtvprops.getPropsArray()[2].u.data];
        return tuneParams;
    }

    public static class TuneStats {
        public int status                      ;
        public boolean statusHasSignal         ;
        public boolean statusHasCarrier        ;
        public boolean statusHasInnerCodeStable;
        public boolean statusHasSync           ;
        public boolean statusHasLock           ;
        public boolean statusTimedOut          ;
        public Float signalStrength_dBm        ;
        public Float signalNoiceRatio_dBm      ;
        public Long  preErrorBitCount          ;
        public Long  preTotalBitCount          ;
        public Long  postErrorBitCount         ;
        public Long  postTotalBitCount         ;
        public Long  errorBlockCount           ;
        public Long  totalBlockCount           ;
    }

    @GetMapping("/tuneStats")
    public TuneStats tuneStats(@RequestParam(defaultValue = "1") int adapter) {
        TuneStats tuneStats = new TuneStats();

        dtv_properties dtvprops = new dtv_properties();
        dtvprops.addStatsCmd(C.CMD.DTV_STAT_SIGNAL_STRENGTH     ); // Index 0
        dtvprops.addStatsCmd(C.CMD.DTV_STAT_CNR                 ); // Index 1
        dtvprops.addStatsCmd(C.CMD.DTV_STAT_PRE_ERROR_BIT_COUNT ); // Index 2
        dtvprops.addStatsCmd(C.CMD.DTV_STAT_PRE_TOTAL_BIT_COUNT ); // Index 3
        dtvprops.addStatsCmd(C.CMD.DTV_STAT_POST_ERROR_BIT_COUNT); // Index 4
        dtvprops.addStatsCmd(C.CMD.DTV_STAT_POST_TOTAL_BIT_COUNT); // Index 5
        dtvprops.addStatsCmd(C.CMD.DTV_STAT_ERROR_BLOCK_COUNT   ); // Index 6
        dtvprops.addStatsCmd(C.CMD.DTV_STAT_TOTAL_BLOCK_COUNT   ); // Index 7
        fe[adapter].feGetProperty(dtvprops);
        // assume scale=1=FE_SCALE_DECIBEL=0.001 dBm
        if (dtvprops.getPropsArray()[0].u.st.stat[0].scale == 1) {
            tuneStats.signalStrength_dBm = dtvprops.getPropsArray()[0].u.st.stat[0].value / 1000f;
        }
        // assume scale=1=FE_SCALE_DECIBEL=0.001 dBm
        if (dtvprops.getPropsArray()[1].u.st.stat[0].scale == 1) {
            tuneStats.signalNoiceRatio_dBm = dtvprops.getPropsArray()[1].u.st.stat[0].value / 1000f;
        }
        // assume scale=3=FE_SCALE_COUNTER
        if (dtvprops.getPropsArray()[2].u.st.stat[0].scale == 3) {
            tuneStats.preErrorBitCount = dtvprops.getPropsArray()[2].u.st.stat[0].value;
        }
        // assume scale=3=FE_SCALE_COUNTER
        if (dtvprops.getPropsArray()[3].u.st.stat[0].scale == 3) {
            tuneStats.preTotalBitCount = dtvprops.getPropsArray()[3].u.st.stat[0].value;
        }
        // assume scale=3=FE_SCALE_COUNTER
        if (dtvprops.getPropsArray()[4].u.st.stat[0].scale == 3) {
            tuneStats.postErrorBitCount = dtvprops.getPropsArray()[4].u.st.stat[0].value;
        }
        // assume scale=3=FE_SCALE_COUNTER
        if (dtvprops.getPropsArray()[5].u.st.stat[0].scale == 3) {
            tuneStats.postTotalBitCount = dtvprops.getPropsArray()[5].u.st.stat[0].value;
        }
        // assume scale=3=FE_SCALE_COUNTER
        if (dtvprops.getPropsArray()[6].u.st.stat[0].scale == 3) {
            tuneStats.errorBlockCount = dtvprops.getPropsArray()[6].u.st.stat[0].value;
        }
        // assume scale=3=FE_SCALE_COUNTER
        if (dtvprops.getPropsArray()[7].u.st.stat[0].scale == 3) {
            tuneStats.totalBlockCount = dtvprops.getPropsArray()[7].u.st.stat[0].value;
        }

        tuneStats.status = fe[adapter].feReadStatus();
        tuneStats.statusHasSignal          = (tuneStats.status & 0x01) > 0;
        tuneStats.statusHasCarrier         = (tuneStats.status & 0x02) > 0;
        tuneStats.statusHasInnerCodeStable = (tuneStats.status & 0x04) > 0;
        tuneStats.statusHasSync            = (tuneStats.status & 0x08) > 0;
        tuneStats.statusHasLock            = (tuneStats.status & 0x10) > 0;
        tuneStats.statusTimedOut           = (tuneStats.status & 0x20) > 0;

        return tuneStats;
    }

    @PostMapping(value = "/stopFrontend")
    public void stopFrontend(@RequestParam(defaultValue = "1") int adapter) {
        if (fe[adapter] != null) {
            fe[adapter].close();
            fe[adapter] = null;
        }
    }

    public static class DocsisStats {
        public int frequency;
        public int countDocsisPackets;
        public int countFillerPackets;
        public int countUnknownPackets;
    }

    @GetMapping("/docsisStats")
    public DocsisStats docsisStats(@RequestParam(defaultValue = "1") int adapter) throws Exception {
        DocsisStats docsisStats = new DocsisStats();
        docsisStats.frequency = (tune(adapter).frequency / 1_000_000) * 1_000_000;

        try (DevDvbDemux dmx = fe[adapter].openDemux()) {
            dmx.dmxSetBufferSize(256 * 1024);

            dmx_pes_filter_params filter = new dmx_pes_filter_params();
            filter.pid = (short) 0x2000;
            filter.input = dmx_pes_filter_params.dmx_input.DMX_IN_FRONTEND;
            filter.output = dmx_pes_filter_params.dmx_output.DMX_OUT_TSDEMUX_TAP;
            filter.pes_type = dmx_pes_filter_params.dmx_ts_pes.DMX_PES_OTHER;
            filter.flags = dmx_sct_filter_params.DMX_IMMEDIATE_START;
            dmx.dmxSetPesFilter(filter);

            long treset = System.nanoTime() +   100_000_000;
            long tmax   = System.nanoTime() + 1_100_000_000;
            byte[] buf = new byte[188 * 5];
            while (System.nanoTime() < tmax) {
                int read = dmx.file.read(buf);
                if ((read % 188) != 0) {
                    throw new RuntimeException("Upps - not multiple of 188");
                }
                for (int i = 0; i < read; i += 188) {
                    if (buf[i] != 0x47) {
                        throw new RuntimeException("Upps - no sync-byte 0x47 at packed start");
                    }

                    int pid = ((buf[i + 1] & 0x1F) << 8) | (buf[i + 2] & 0xFF);
                    if (pid == 0x1FFE) {
                        docsisStats.countDocsisPackets++;
//                        DatagramPacket udp = new DatagramPacket(buf, i, 188,addr);
//                        udpSocket.send(udp);
                    } else if (pid == 0x1FFF) {
                        docsisStats.countFillerPackets++;
                    } else {
                        docsisStats.countUnknownPackets++;
                    }
                }
                if (System.nanoTime() < treset) {
                    docsisStats.countDocsisPackets = 0;
                    docsisStats.countFillerPackets = 0;
                    docsisStats.countUnknownPackets = 0;
                }
            }
        }

        return docsisStats;
    }

    @GetMapping(value = "/sct/{pid}")
    public void streamSections(@PathVariable int pid, @RequestParam(defaultValue = "1") int adapter, OutputStream os) {
        try (DevDvbDemux dmx = fe[adapter].openDemux()) {
            dmx.dmxSetBufferSize(256 * 1024);

            dmx_sct_filter_params filter = new dmx_sct_filter_params();
            filter.pid = (short) pid;
            filter.flags = dmx_sct_filter_params.DMX_CHECK_CRC | dmx_sct_filter_params.DMX_IMMEDIATE_START;
            filter.timeout = 1_000;
            dmx.dmxSetFilter(filter);

            new DocsicDecoder(dmx, os).decode();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @GetMapping(value = "/pes/{pid}", produces = "video/mpeg2video")
    public void stream(@PathVariable int pid, @RequestParam(defaultValue = "1") int adapter, OutputStream os) {
        try (DevDvbDemux dmx = fe[adapter].openDemux()) {
            dmx.dmxSetBufferSize(256 * 1024);

            dmx_pes_filter_params filter = new dmx_pes_filter_params();
            filter.pid = (short) pid;
            filter.input = dmx_pes_filter_params.dmx_input.DMX_IN_FRONTEND;
            filter.output = dmx_pes_filter_params.dmx_output.DMX_OUT_TAP;
//            filter.output = dmx_pes_filter_params.dmx_output.DMX_OUT_TSDEMUX_TAP;
            filter.pes_type = dmx_pes_filter_params.dmx_ts_pes.DMX_PES_OTHER;
            filter.flags = dmx_sct_filter_params.DMX_IMMEDIATE_START;
            dmx.dmxSetPesFilter(filter);

//            new DecodeTeletext().decode(dmx, resp.getOutputStream());
            new Mpeg2videoDecoderNeu(dmx, os).decode();
//            new H264Decoder(dmx, resp.getOutputStream()).decode();
//            new DocsicDecoder(dmx, resp.getOutputStream()).decode();

//            MyBAOS baos = new MyBAOS();
//            byte[] buf = new byte[4 * 1024];
//            while (true) {
//                while (true) {
//                    int skipped = 0;
//                    while (true) {
//                        int i = dmx.file.readByte();
//                        if (i == 0) {
//                            break;
//                        }
//                        skipped++;
//                    }
//                    if (skipped > 0) {
////                        System.out.println("hmmm need to skip " + skipped);
//                    }
//                    if (dmx.file.readByte() == 0) {
//                        if (dmx.file.readByte() == 1) {
//                            break;
//                        }
//                    }
//                }
//                int streamId = dmx.file.readByte() & 0xFF;
//                if (streamId != 0xE0) {
//                    continue;
//                }
//                int pesPacketLen = dmx.file.readShort() & 0xFFFF;
//                if (pesPacketLen != 0) {
//                    continue;
//                }
//                System.out.printf("streamId=%02x, pesPacketLen=%d", streamId, pesPacketLen);
//                baos.reset();
//                for (int remain = pesPacketLen; remain > 0; ) {
//                    int read = dmx.file.read(buf, 0, Math.min(buf.length, remain));
//                    baos.write(buf, 0, read);
//                    remain -= read;
//                }
//                PacketReader pr = baos.asPacketReader();
//                int pesHeader1   = pr.pull8();
//                int pesHeader2   = pr.pull8();
//                int pesHeaderLen = pr.pull8();
//                System.out.printf(" hdr1=%02x hdr2=%02x pesHeaderLen=%d\n", pesHeader1, pesHeader2, pesHeaderLen);
//                pr.skip(pesHeaderLen);
//
//                byte[] elementaryStream = pr.nextBytesAsPR(pr.remain()).wholePacket();
//                resp.getOutputStream().write(elementaryStream);
//                resp.getOutputStream().flush();
////                PacketDecoderDVB.hexDump(elementaryStream);
//                System.out.println();
//            }

//            for (int i = 0; i < 1_000_000; i++) {
//                int read = dmx.file.read(buf);
//                System.out.printf("read=%d\n", read);
//                resp.getOutputStream().write(buf, 0, read);
//                resp.getOutputStream().flush();
//            }


        } catch (Exception ex) {
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

    @PostMapping("/startPidReceiverIfNotYetStarted/{pid}")
    public void startPidReceiverIfNotYetStarted(@PathVariable int pid, @RequestParam(defaultValue = "1") int adapter) {
        pidReceivers.computeIfAbsent(pid, newPid -> {
            PidReceiver pr = new PidReceiver(adapter, newPid);
            pr.start();
            return pr;
        });
    }

    public class PidReceiver extends Thread {

        private final int adapter;
        private final int pid;
        private final DevDvbDemux dmx;

        public PidReceiver(int adapter, int pid) {
            this.adapter = adapter;
            this.pid = pid;
            this.dmx = fe[adapter].openDemux();

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
            pidReceivers.remove(pid);
        }

        public void close() {
            dmx.close();
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
                    decoderPAT.decode(prSection, something, OliController.this, adapter);
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
}



