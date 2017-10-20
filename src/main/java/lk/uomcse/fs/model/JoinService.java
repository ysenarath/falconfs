package lk.uomcse.fs.model;

import lk.uomcse.fs.entity.Node;
import lk.uomcse.fs.messages.IMessage;
import lk.uomcse.fs.messages.IRequest;
import lk.uomcse.fs.messages.JoinRequest;
import lk.uomcse.fs.messages.JoinResponse;
import org.apache.log4j.Logger;

import java.util.Set;

public class JoinService extends Thread {
    private final static Logger LOGGER = Logger.getLogger(JoinService.class.getName());

    private boolean running;

    private final RequestHandler handler;

    private final Node current;

    private final Set<Node> neighbours;

    /**
     * Allocates Join service object.
     *
     * @param handler    Request handler
     * @param current    Current node running this join service
     * @param neighbours reference to neighbours
     */
    public JoinService(RequestHandler handler, Node current, Set<Node> neighbours) {
        this.handler = handler;
        this.current = current;
        this.neighbours = neighbours;
    }

    /**
     * Thread function
     * Handles incoming join messages
     */
    @Override
    public void run() {
        running = true;
        LOGGER.trace(String.format("Starting join service for node at (%s:%d).", current.getIp(), current.getPort()));
        while (running) {
            String msg = this.handler.receiveMessage(JoinRequest.ID);
            JoinRequest request = JoinRequest.parse(msg);
            IMessage reply = new JoinResponse(true);
            LOGGER.info(String.format("Replying to join request: %s", reply.toString()));
            // Request handling section
            this.handler.sendMessage(request.getNode().getIp(), request.getNode().getPort(), reply);
            neighbours.add(request.getNode());
            LOGGER.info(String.format("Node(%s:%d) is joined to nodes: %s", current.getIp(), current.getPort(), neighbours.toString()));
        }
    }

    /**
     * Joins to provided node and add it as a neighbour
     *
     * @param n a node to join
     * @return whether join request is success or not
     */
    public boolean join(Node n) {
        IRequest jr = new JoinRequest(current);
        LOGGER.info(String.format("Requesting node(%s:%d) to join: %s", n.getIp(), n.getPort(), jr.toString()));
        handler.sendMessage(n.getIp(), n.getPort(), jr);
        LOGGER.debug("Waiting for receive message.");
        String reply = handler.receiveMessage(JoinResponse.ID);
        LOGGER.info(String.format("Replied to join request: %s", reply));
        JoinResponse rsp = JoinResponse.parse(reply);
        if (rsp.isSuccess())
            neighbours.add(n);
        return rsp.isSuccess();
    }

    /**
     * Changes state of execution
     *
     * @param running whether to run/stop this thread
     */
    public void setRunning(boolean running) {
        this.running = running;
    }
}