package lk.uomcse.fs.entity;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;

public class Neighbour implements Comparator<Neighbour>, Comparable<Neighbour> {

    private Node node;

    //health of the node. should be in between 0 and 100
    private Integer health;

    /**
     * BeatCount of a node.
     */
    private ArrayList<Long> pulseResponses;

    public Neighbour(Node node) {
        this.node = node;
        Random rand = new Random();

        this.health = rand.nextInt(100) + 1;
        this.pulseResponses = new ArrayList<Long>();
    }

    public Neighbour(String ip, int port) {
        this.node = new Node(ip, port);
        Random rand = new Random();

        this.health = rand.nextInt(100) + 1;
        this.pulseResponses = new ArrayList<Long>();
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        if (health > 10) {
            this.health = 10;
        } else if (health < 0) {
            this.health = 0;
        } else {
            this.health = health;
        }
    }

    /**
     * Returns the pulse count and clears the out of frame {@code pulseResponses}
     *
     * @return {@code count}
     */
    public synchronized int getPulseCount() {
        long currentTime = System.currentTimeMillis() - 5000;
        int size = pulseResponses.size();
        int count = 0;
        for (int i = size - 1; i >= 0; i--) {
            if (pulseResponses.get(i) > currentTime) {
                count++;
            }
        }
        if (count != size) {
            pulseResponses = new ArrayList<Long>(pulseResponses.subList(count, size));
        }
        return count;
    }

    /**
     * Updates the {@code pulseResponses}
     */
    public synchronized void addPulseResponse(long time) {
        pulseResponses.add(time);
    }


    @Override
    public int compare(Neighbour o1, Neighbour o2) {
        return o1.health - o2.health;
    }

    @Override
    public int compareTo(Neighbour o) {
        return (o.health).compareTo(this.health);
    }

    public Node getNode() {
        return node;
    }


    @Override
    public String toString() {
        return node.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Node) {
            return node.equals(o);
        } else if (o instanceof Neighbour)
            return node.equals(((Neighbour) o).getNode());
        return false;
    }

    @Override
    public int hashCode() {
        return node.hashCode();
    }

}