package via.vinylsystem.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * TCP client for interacting with a Vinyl catalog server.
 * <p>
 * Provides methods to list all tracks, search for tracks by query, retrieve
 * a track by ID, and manage the TCP connection. All communication with the
 * server uses JSON messages over TCP.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * VinylServerClient client = new VinylServerClient("127.0.0.1", 7070);
 * JsonObject tracks = client.list();
 * JsonObject hits = client.search("Gilli");
 * String track = client.get("T001");
 * client.close();
 * </pre>
 * </p>
 *
 * <p>
 * All methods throw {@link IOException} in case of network errors or
 * server communication issues.
 * </p>
 *
 * @author Ghiyath & sakariae
 * @version 1.0
 */
public class VinylServerClient implements Closeable
{
    /** TCP socket for communication with the Vinyl server. */
    private final Socket sock;

    /** Buffered reader for reading JSON responses from the server. */
    private final BufferedReader in;

    /** Buffered writer for sending JSON requests to the server. */
    private final BufferedWriter out;

    /**
     * Constructs a new TCP client connected to the specified Vinyl server.
     *
     * @param ip the server IP address
     * @param port the server TCP port
     * @throws IOException if the connection cannot be established
     */
    public VinylServerClient(String ip, int port) throws IOException
    {
        this.sock = new Socket();
        this.sock.connect(new InetSocketAddress(ip, port), 2000);
        this.in = new BufferedReader(new InputStreamReader(sock.getInputStream(),
                StandardCharsets.UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8));
    }

    /**
     * Sends a JSON message to the server and waits for a JSON response.
     *
     * @param msg the JSON object to send
     * @return the JSON object received in response
     * @throws IOException if a network error occurs or server closes the connection
     */
    private JsonObject send(JsonObject msg) throws IOException
    {
        out.write(msg.toString());
        out.write("\n");
        out.flush();
        String line = in.readLine();
        if(line == null) throw new EOFException("Server closed");
        return JsonParser.parseString(line).getAsJsonObject();
    }

    /**
     * Requests the full list of tracks from the server.
     *
     * @return a {@link JsonObject} containing all tracks
     * @throws IOException if a network error occurs
     */
    public JsonObject list() throws IOException
    {
        JsonObject q = new JsonObject();
        q.addProperty("CMD", "LIST");
        return send(q);
    }

    /**
     * Searches for tracks on the server by a query string.
     *
     * @param qstr the search query (artist or title)
     * @return a {@link JsonObject} containing matching tracks
     * @throws IOException if a network error occurs
     */
    public JsonObject search(String qstr) throws IOException
    {
        JsonObject q = new JsonObject();
        q.addProperty("CMD", "SEARCH");
        q.addProperty("Q", qstr);
        return send(q);
    }

    /**
     * Retrieves a specific track by its ID.
     *
     * @param id the track ID
     * @return a JSON string representing the track
     * @throws IOException if a network error occurs
     */
    public String get(String id) throws IOException
    {
        JsonObject q = new JsonObject();
        q.addProperty("CMD", "GET");
        q.addProperty("ID", id);
        return send(q).toString();
    }

    /**
     * Closes the TCP connection to the Vinyl server.
     *
     * @throws IOException if an error occurs while closing the socket
     */
    @Override
    public void close() throws IOException
    {
        sock.close();
    }
}
