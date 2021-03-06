package lk.uomcse.fs.model;

import lk.uomcse.fs.utils.TextFormatUtils;
import lk.uomcse.fs.utils.error.BsFullError;
import lk.uomcse.fs.utils.error.ErrorInCommand;
import lk.uomcse.fs.messages.RegisterRequest;
import lk.uomcse.fs.messages.RegisterResponse;
import lk.uomcse.fs.messages.UnregisterRequest;
import lk.uomcse.fs.messages.UnregisterResponse;
import lk.uomcse.fs.entity.BootstrapServer;
import lk.uomcse.fs.entity.Node;
import lk.uomcse.fs.utils.exceptions.BootstrapException;
import lk.uomcse.fs.utils.exceptions.RequestFailedException;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.concurrent.TimeoutException;

public class BootstrapService {
    private final static Logger LOGGER = Logger.getLogger(BootstrapService.class.getName());

    private final static int MAX_RETRIES = 3;

    // -----------------------------------------------------------------------------------------------------------------

    private final BootstrapServer server;

    private final JoinService joinService;

    private final RequestHandler handler;

    private final String name;

    private final Node self;

    /**
     * Constructs bootstrap service providing register and unregister functions
     *
     * @param handler     Request handler
     * @param joinService join service to join to nodes ones the registration is complete
     * @param bs          Bootstrap server (details)
     * @param name        name of the client
     * @param self        node represented by the name
     */
    public BootstrapService(RequestHandler handler, JoinService joinService, BootstrapServer bs, String name, Node self) {
        this.server = bs;
        this.handler = handler;
        this.joinService = joinService;
        this.name = name;
        this.self = self;
    }

    /**
     * Registers the node bootstrap from server
     *
     * @return List of nodes if the request is successful
     */
    public List<Node> register() throws BootstrapException {
        RegisterRequest msg = new RegisterRequest(name, self);
        LOGGER.info(String.format("Requesting bootstrap server: %s", msg.toString()));
        String reply = null;
        int retries = 0;
        while (true)
            try {
                this.handler.sendMessage(this.server.getHost(), this.server.getPort(), msg);
                reply = this.handler.receiveMessage(RegisterResponse.ID, 5);
                break;
            } catch (TimeoutException e) {
                if (retries < MAX_RETRIES) {
                    LOGGER.debug("Failed the " + TextFormatUtils.toRankedText(retries + 1) + " attempt to register");
                    retries++;
                } else {
                    LOGGER.error("Failed the attempt to register. Unable to receive message from bootstrap.");
                    throw new BootstrapException("Failed the attempt to register. Unable to receive message from bootstrap.");
                }
            }
        LOGGER.info(String.format("Bootstrap server replied: %s", reply));
        RegisterResponse rsp = RegisterResponse.parse(reply);
        Error err;
        if (rsp.isSuccess()) {
            // TODO: Select random 2 and return
            return rsp.getNodes();
        } else {
            switch (rsp.getNodeCount()) {
                case (9998):
                    boolean status = this.unregister();
                    if (!status) throw new BootstrapException("Un-registration failed. Unable to bootstrap.");
                    return this.register();
                case (9999):
                    err = new ErrorInCommand.Builder(9999)
                            .setError("failed, there is some error in the command")
                            .build();
                    break;
                case (9997):
                    err = new BsFullError.Builder(9997)
                            .setError("failed, registered to another user, try a different IP and port")
                            .build();
                    break;
                case (9996):
                    err = new BsFullError.Builder(9996)
                            .setError("failed, can’t register. BS full.")
                            .build();
                    break;
                default:
                    throw new UnknownError("Unknown error code received from bootstrap server.");
            }
        }
        throw new BootstrapException(err.getMessage());
    }

    /**
     * Unregisters the node bootstrap from server
     *
     * @return whether the response is success
     */
    public boolean unregister() throws BootstrapException {
        UnregisterRequest msg = new UnregisterRequest(name, self);
        LOGGER.info(String.format("Requesting Bootstrap Server: %s", msg.toString()));
        String reply;
        int count = 0;
        //try again for three times to unregister
        while (count < MAX_RETRIES) {
            try {
                // Method will wait for reply
                this.handler.sendMessage(this.server.getHost(), this.server.getPort(), msg);
                reply = this.handler.receiveMessage(UnregisterResponse.ID, 5);
                LOGGER.info(String.format("Bootstrap Server replied: %s", reply));
                UnregisterResponse rsp = UnregisterResponse.parse(reply);
                return rsp.isSuccess();
            } catch (TimeoutException e) {
                count += 1;
                LOGGER.error("Failed the " + count + " attempt to unregister");
            }
        }
        throw new BootstrapException("Failed to unregister node. No reply received from bootstrap server.");
    }


    /**
     * Connects with bootstrap server and joins to nodes provided
     *
     * @return whether bootstrap is a success
     */
    public boolean bootstrap() {
        try {
            List<Node> nodes = this.register();
            nodes.forEach(joinService::join);
            for (Node n : nodes) {
                boolean status = joinService.join(n);
                if (status)
                    LOGGER.info(String.format("Joined to neighbour: %s", n.toString()));
                else
                    LOGGER.error(String.format("Failed join to neighbour: %s", n.toString()));
            }
        } catch (RequestFailedException | BootstrapException ex) {
            return false;
        }
        return true;
    }
}
