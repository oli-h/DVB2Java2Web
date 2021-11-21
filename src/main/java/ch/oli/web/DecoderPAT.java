package ch.oli.web;

import ch.oli.decode.PacketReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DecoderPAT {

    @Autowired
    private ServerWebSocketHandler serverWebSocketHandler;

    public void decode(PacketReader prSection, int something, OliController oliController) {
        ProgramAssociation pa = new ProgramAssociation(); // re-use in loop
        while (prSection.remain() > 4) {
            pa.service_id = prSection.pull16();
            pa.pmt_pid    = prSection.pull16() & 0x1FFF;
            serverWebSocketHandler.broadcast(pa);
            oliController.startPidReceiverIfNotYetStarted(pa.pmt_pid);
        }
        // 4 bytes remain CRC - ignored
    }

    public static class ProgramAssociation {
        public String type = "pat";
        public int service_id;
        public int pmt_pid;
    }

}
