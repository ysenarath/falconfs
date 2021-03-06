package lk.uomcse.fs.model;

import lk.uomcse.fs.entity.Node;
import lk.uomcse.fs.messages.HeartbeatPulse;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * The {@code HeartbeatService} class represents UDP Heartbeats.
 * Sends heartbeats to every neighbor in each {@code SLEEP_TIME}
 *
 * @author Dulanjaya Tennekoon
 * @see Node
 * @see HeartbeatPulse
 * @see RequestHandler
 * @since Phase1
 */
public class HeartbeatService extends Thread {
    private final static Logger LOGGER = Logger.getLogger(BootstrapService.class.getName());

    // Pulse Rate of the Heart Beats.
    private final static int SLEEP_TIME = 1000;

    // -----------------------------------------------------------------------------------------------------------------

    // Heartbeat Pulse of each heartbeat
    private final HeartbeatPulse pulse = new HeartbeatPulse();

    // Neighbors are the neighbor-nodes of the self-node.
    private final List<Node> neighbors;

    // Request Handler of the heartbeats.
    private final RequestHandler requestHandler;

    // -----------------------------------------------------------------------------------------------------------------

    // Activation of the heartbeat
    private boolean pulseBeating = true;

    /**
     * Initializes a new {@code HeartBeat} object.
     * Note that heart beats are done from self node to routing nodes.
     *
     * @param requestHandler requestHandler of the Self-Node
     * @param neighbors      Neighbors list of the Self-Node
     */
    public HeartbeatService(RequestHandler requestHandler, List<Node> neighbors) {
        this.requestHandler = requestHandler;
        this.neighbors = neighbors;
    }

    /**
     * Starts the heart beating thread.
     */
    @Override
    public void run() {
        while (this.pulseBeating) {
            this.sendPulses();
        }
    }

    /**
     * Send heartbeat pulses to every neighbor.
     */
    private void sendPulses() {
        try {
            for (Node neighbor : neighbors) {
                LOGGER.info("Sending Heartbeat Message:" + neighbor.getIp());
                this.requestHandler.sendMessage(neighbor.getIp(), neighbor.getPort(), this.pulse);
            }
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
            LOGGER.error(e);
        }

    }

    /**
     * Makes the heart activated or de-activated.
     *
     * @param activate activates/deactivates heart beating.
     */
    public void setPulseBeating(boolean activate) {
        this.pulseBeating = activate;
    }
}
