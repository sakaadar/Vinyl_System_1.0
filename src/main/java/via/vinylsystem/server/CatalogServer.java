package via.vinylsystem.server;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import via.vinylsystem.Model.Track;
import via.vinylsystem.directory.StatusCodes;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TCP server providing catalog services for the Vinyl System.
 * <p>
 * This server manages a music track catalog and handles client requests to list,
 * search, and retrieve track information. Each client connection is handled in
 * a separate thread using a cached thread pool for concurrent request processing.
 * </p>
 * <p>
 * Supported commands:
 * <ul>
 *   <li>LIST - Returns all tracks in the catalog</li>
 *   <li>SEARCH - Searches for tracks by artist or title</li>
 *   <li>GET - Retrieves a specific track by ID</li>
 *   <li>QUIT - Closes the client connection</li>
 * </ul>
 * </p>
 * <p>
 * All requests and responses are in JSON format, sent as newline-delimited messages.
 * The server maintains a persistent connection with each client until QUIT is received
 * or the connection is closed.
 * </p>
 *
 * @author Ghiyath & sakariae
 * @version 1.0
 */
public class CatalogServer
{
    private final int servicePort;
    private ServerSocket server;
    private ExecutorService clientPool;
    private volatile boolean running;
    private List<via.vinylsystem.Model.Track> catalog;

    private static final Gson GSON = new Gson();

    /**
     * Constructs a new CatalogServer with the specified port and catalog.
     * <p>
     * If no catalog is provided (null), a default seed catalog with Danish music
     * tracks will be used.
     * </p>
     *
     * @param servicePort the port number on which the server will listen for connections
     * @param catalog the list of tracks to serve, or null to use the default seed catalog
     */
    public CatalogServer(int servicePort, List<via.vinylsystem.Model.Track> catalog)
    {
        this.servicePort = servicePort;
        this.catalog = (catalog != null) ? catalog : seedCatalog();
        this.running = false;
    }

    /**
     * Starts the catalog server and begins accepting client connections.
     * <p>
     * Creates a server socket on the configured port and initializes a cached
     * thread pool for handling client connections concurrently. The accept loop
     * runs in a separate thread named "catalog-accept".
     * </p>
     *
     * @throws IOException if the server socket cannot be created or bound
     */
    public void start() throws IOException
    {
        server = new ServerSocket(servicePort);
        clientPool = Executors.newCachedThreadPool();
        running = true;

        Thread t = new Thread(this::acceptLoop, "catalog-accept");
        t.start();
    }

    /**
     * Stops the catalog server and closes the server socket.
     * <p>
     * This method stops accepting new connections and closes the server socket.
     * Existing client connections will complete their current operations.
     * </p>
     */
    public void stop(){
        running = false;
        try {
            if(server != null && !server.isClosed())
            {
                server.close();
            }
        }
        catch (IOException e)
        {
            System.out.println("Could not close server: " + e);
        }
    }

    /**
     * Main server loop that continuously accepts incoming client connections.
     * <p>
     * Each accepted connection is submitted to the thread pool for concurrent
     * processing. This method runs until the server is stopped.
     * </p>
     */
    private void acceptLoop()
    {
        while(running)
        {
            try
            {
                Socket s = server.accept();
                clientPool.submit(()->handleClient(s));
            }
            catch (IOException e)
            {
                if(running)
                {
                    throw new RuntimeException("Could not accept connections: " + e);
                }
            }
        }
    }

    /**
     * Handles an individual client connection with persistent session support.
     * <p>
     * Processes JSON commands from the client in a loop until QUIT is received
     * or the connection is closed. Each command receives an immediate JSON response.
     * </p>
     * <p>
     * Command formats:
     * <ul>
     *   <li>LIST: {"CMD":"LIST"}</li>
     *   <li>SEARCH: {"CMD":"SEARCH", "Q":"query"}</li>
     *   <li>GET: {"CMD":"GET", "ID":"trackId"}</li>
     *   <li>QUIT: {"CMD":"QUIT"}</li>
     * </ul>
     * </p>
     * <p>
     * Response format: {"STATUS":"statusCode", ...additional fields...}
     * </p>
     *
     * @param s the client socket connection to handle
     */
    private void handleClient(Socket s) {
        BufferedReader in = null;
        BufferedWriter out = null;
        try {
            s.setSoTimeout(0);
            in  = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));


            while (true) {
                String line = in.readLine();
                if (line == null) break;
                System.out.println("CAT IN " + line);

                JsonObject req = JsonParser.parseString(line).getAsJsonObject();
                String cmd = req.has("CMD") ? req.get("CMD").getAsString() : "";

                if ("QUIT".equalsIgnoreCase(cmd)) {
                    JsonObject bye = new JsonObject();
                    bye.addProperty("STATUS", StatusCodes.OK);
                    out.write(bye.toString()); out.write('\n'); out.flush();
                    break;
                }

                JsonObject resp = new JsonObject();
                resp.addProperty("STATUS", StatusCodes.OK);

                switch (cmd) {
                    case "LIST" -> resp.add("TRACKS", GSON.toJsonTree(catalog));
                    case "SEARCH" -> {
                        String q = req.has("Q") ? req.get("Q").getAsString() : "";
                        List<Track> hits = catalog.stream()
                                .filter(t -> t.getArtist().toLowerCase().contains(q.toLowerCase())
                                        || t.getTitle().toLowerCase().contains(q.toLowerCase()))
                                .toList();
                        resp.add("TRACKS", GSON.toJsonTree(hits).getAsJsonArray());
                    }
                    case "GET" -> {
                        String id = req.has("ID") ? req.get("ID").getAsString() : "";
                        if (id.isBlank()) resp.addProperty("STATUS", StatusCodes.BAD_REQUEST);
                        else {
                            Track t = findById(catalog, id);
                            if (t == null) resp.addProperty("STATUS", StatusCodes.NOT_FOUND);
                            else resp.add("TRACK", GSON.toJsonTree(t));
                        }
                    }
                    default -> resp.addProperty("STATUS", StatusCodes.UNKNOWN_CMD);
                }

                System.out.println("CAT OUT " + resp);
                out.write(resp.toString()); out.write('\n'); out.flush();
            }
        } catch (Exception e) {
            System.err.println("CatalogServer error: " + e);
            if (out != null) {
                try {
                    JsonObject err = new JsonObject();
                    err.addProperty("STATUS", StatusCodes.SERVER_ERROR); // "000500"
                    out.write(err.toString()); out.write('\n'); out.flush();
                } catch (IOException ignore) {}
            }
        } finally {
            try { if (out != null) out.close(); } catch (IOException ignore) {}
            try { if (in  != null) in.close();  } catch (IOException ignore) {}
            try { s.close(); } catch (IOException ignore) {}
        }
    }

    /**
     * Finds a track in the catalog by its ID.
     * <p>
     * Performs a case-insensitive search through the catalog for a track
     * with the matching ID.
     * </p>
     *
     * @param catalog the list of tracks to search
     * @param id the track ID to find
     * @return the matching Track object, or null if not found
     */
    private Track findById(List<Track> catalog, String id)
    {
        for(Track track: catalog)
        {
            if(track.getId().equalsIgnoreCase(id)){
                return track;
            }
        }
        return null;
    }

    /**
     * Creates a default seed catalog with Danish music tracks.
     * <p>
     * This catalog contains 10 popular Danish songs from various artists,
     * used when no custom catalog is provided during server construction.
     * </p>
     *
     * @return an immutable list of Track objects representing the seed catalog
     */
    public static List<Track> seedCatalog(){
        return List.of(
                new Track("T001","Gilli", "La Varrio", 2017),
                new Track("T002", "Kesi", "Mamacita", 2014),
                new Track("T003", "Medina", "Kun for mig", 2009),
                new Track("T004", "L.O.C.", "Frk. Escobar", 2004),
                new Track("T005", "Nik & Jay", "Boing!", 2006),
                new Track("T006", "Malk de Koijn", "Vi tager fuglen på dig", 2002),
                new Track("T007", "Suspekt", "Søndagsbarn", 2014),
                new Track("T008", "Barbara Moleko", "Dum for dig", 2012),
                new Track("T009", "Christopher", "Told You So", 2014),
                new Track("T010", "Rasmus Seebach", "Lidt i fem", 2009)
        );
    }
}