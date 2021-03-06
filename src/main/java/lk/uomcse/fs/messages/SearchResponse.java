package lk.uomcse.fs.messages;

import lk.uomcse.fs.entity.Node;
import lk.uomcse.fs.utils.exceptions.InvalidFormatException;

import java.util.Arrays;
import java.util.List;

// length SEROK qid no_files IP port hops filename1 filename2 ... ...
public class SearchResponse implements IResponse {
    public static final String ID = "SEROK";

    private String queryID;

    private int fileCount;

    private Node node;

    private int hops;

    private List<String> filenames;

    /**
     * Cstr
     *
     * @param fileCount Number of results returned
     * @param node      Node having (stored) the file.
     * @param hops      Hops required to find the file(s).
     * @param filenames Actual names of the files.
     */
    public SearchResponse(String queryID, int fileCount, Node node, int hops, List<String> filenames) {
        this.queryID = queryID;
        this.fileCount = fileCount;
        this.node = node;
        this.hops = hops;
        this.filenames = filenames;
    }

    /**
     * Number of results returned
     *
     * @return ≥ 1 – Successful
     * 0 – no matching results. Searched key is not in key table
     * 9999 – failure due to node unreachable
     * 9998 – some other error.
     */
    public int getFileCount() {
        return fileCount;
    }

    /**
     * Node having (stored) the file.
     *
     * @return a node having (stored) the file.
     */
    public Node getNode() {
        return node;
    }

    /**
     * Hops required to find the file(s).
     *
     * @return hops required to find the file(s).
     */
    public int getHops() {
        return hops;
    }

    /**
     * Actual names of the files.
     *
     * @return file names.
     */
    public List<String> getFilenames() {
        return filenames;
    }

    /**
     * Gets query ID
     *
     * @return query id
     */
    public String getQueryID() {
        return queryID;
    }

    /**
     * Parses search response message
     *
     * @param reply reply in string
     * @return Unregister response message
     */
    public static SearchResponse parse(String reply) {
        if (reply == null)
            throw new NullPointerException();
        String[] response = reply.split(" ");
        if (response.length < 6)
            throw new InvalidFormatException("Parsing failed due to not having enough content to match the format.");
        if (!response[1].equals(ID))
            throw new InvalidFormatException(String.format("Parsing failed due to not having message id: %s. (Received message ID: %s)", ID, response[1]));
        String qid = response[2];
        int n = Integer.parseInt(response[3]);
        String ip = response[4];
        int port = Integer.parseInt(response[5]);
        int hops = Integer.parseInt(response[6]);
        // TODO: Handle Errors
        List<String> filenames = Arrays.asList(response).subList(7, response.length);
        return new SearchResponse(qid, n, new Node(ip, port), hops, filenames);
    }

    /**
     * To string method
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(" ");
        sb.append(ID).append(" ")
                .append(this.queryID).append(" ")
                .append(this.fileCount).append(" ")
                .append(node.getIp()).append(" ")
                .append(node.getPort()).append(" ")
                .append(this.hops).append(" ")
                .append(String.join(" ", this.filenames));
        String length = String.format("%04d", sb.length() + 4);
        sb.insert(0, length);
        return sb.toString();
    }
}
