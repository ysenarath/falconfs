package lk.uomcse.fs.model.com;

import lk.uomcse.fs.model.messages.IMessage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class UDPSender extends Sender {
    private static final int MAX_RETRIES = 3;

    private DatagramSocket socket;

    private BlockingQueue<DatagramPacket> packets;

    /**
     * Creates the part of client that handles sends
     *
     * @param socket Datagram socket
     */
    public UDPSender(DatagramSocket socket) {
        this.packets = new LinkedBlockingQueue<>();
        this.socket = socket;
    }

    /**
     * Thread function
     */
    @Override
    public void run() {
        running = true;
        int retries;
        while (running) {
            DatagramPacket packet;
            try {
                packet = this.packets.take();
            } catch (InterruptedException e) {
                continue;
            }
            retries = 0;
            while (retries < MAX_RETRIES) {
                try {
                    this.socket.send(packet);
                    break;
                } catch (IOException e) {
                    retries++;
                }
            }
        }
    }


    /**
     * Requests given node
     *
     * @param ip      ip of the requested node
     * @param port    port of the requested node
     * @param request request
     */
    @Override
    public void send(String ip, int port, IMessage request) {
        System.out.println(String.format("Packet being sent to %s:%d - %s", ip, port, request.toString()));
        byte[] buf = request.toString().getBytes();
        InetAddress address;
        try {
            address = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            // TODO: Create custom exception + handle correctly
            throw new RuntimeException("The IP address of a host could not be determined.");
        }
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
        this.packets.add(packet);
    }
}