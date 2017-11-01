package lk.uomcse.fs.model;

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class BootstrapService {
    private final static Logger LOGGER = Logger.getLogger(BootstrapService.class.getName());

    private final static int MAX_RETRIES = 3;

    private final BootstrapServer server;

    private RequestHandler handler;

    /**
     * Constructs bootstrap service providing register and unregister functions
     *
     * @param handler Request handler
     * @param bs      Bootstrap server (details)
     */
    public BootstrapService(RequestHandler handler, BootstrapServer bs) {
        this.server = bs;
        this.handler = handler;
    }

    /**
     * Registers the node bootstrap from server
     *
     * @param name name of the client
     * @param me   node represented by the name
     * @return List of nodes if the request is successful
     */
    public List<Node> register(String name, Node me) throws BootstrapException {
        RegisterRequest msg = new RegisterRequest(name, me);
        LOGGER.info(String.format("Requesting bootstrap server: %s", msg.toString()));
        String reply = null;
        int retries = 0;
        while (true)
            try {
                this.handler.sendMessage(this.server.getHost(), this.server.getPort(), msg);
                reply = this.handler.receiveMessage(RegisterResponse.ID, 500);
                break;
            } catch (TimeoutException e) {
                if (retries < MAX_RETRIES) {
                    LOGGER.error("Failed the " + retries + 1 + " attempt to unregister");
                    retries++;
                } else {
                    LOGGER.error("Failed the attempt to register. Unable to recieve message from bootstrap.");
                    throw new BootstrapException("Failed the attempt to register. Unable to recieve message from bootstrap.");
                }
            }
        LOGGER.info(String.format("Bootstrap server replied: %s", reply));
        RegisterResponse rsp = RegisterResponse.parse(reply);
        if (rsp.isSuccess())
        // TODO: Select random 2 and return
        {
            return rsp.getNodes();
        } else {
            switch (rsp.getNodeCount()) {
                case (9998):
                    this.unregister(name, me);
                    return this.register(name, me);
                case (9999):
                    new ErrorInCommand.Builder(9999)
                            .setError("failed, there is some error in the command")
                            .build()
                            .handleError();
                case (9997):
                    new BsFullError.Builder(9997)
                            .setError("failed, registered to another user, try a different IP and port")
                            .build()
                            .handleError();
                case (9996):
                    new BsFullError.Builder(9996)
                            .setError("failed, can’t register. BS full.")
                            .build()
                            .handleError();
                default:
                    break;
            }
        }
        // TODO: Handle other request errors
        throw new RequestFailedException("Unhandled request error!");
    }

    /**
     * Unregisters the node bootstrap from server
     *
     * @param name name of the client
     * @param me   node represented by the name
     * @return whether the response is success
     */
    public boolean unregister(String name, Node me) throws BootstrapException {
        UnregisterRequest msg = new UnregisterRequest(name, me);
        LOGGER.info(String.format("Requesting Bootstrap Server: %s", msg.toString()));
        String reply;
        boolean tryAgain = true;
        int count = 0;
        //try again for three times to unregister
        while (tryAgain && count < 4) {
            try {
                // Method will wait for reply
                this.handler.sendMessage(this.server.getHost(), this.server.getPort(), msg);
                reply = this.handler.receiveMessage(UnregisterResponse.ID, 5);
                LOGGER.info(String.format("Bootstrap Server replied: %s", reply));
                UnregisterResponse rsp = UnregisterResponse.parse(reply);
                tryAgain = false;
                return rsp.isSuccess();
            } catch (TimeoutException e) {
                count += 1;
                LOGGER.error("Failed the " + count + " attempt to unregister");
            }
        }
        return false;

    }
}
