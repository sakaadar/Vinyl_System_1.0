package via.vinylsystem.directory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static via.vinylsystem.Util.JsonUtils.*;

/**
 * TCP server for handling directory service registration and renewal requests.
 * <p>
 * This server accepts JSON-formatted commands from clients to register or update
 * their presence in the directory service. Each client connection is handled in
 * a separate thread using a cached thread pool.
 * </p>
 * <p>
 * Supported commands:
 * <ul>
 *   <li>REGISTER - Register a new service with a name and IP address</li>
 *   <li>RENEW/UPDATE - Renew or update an existing service registration</li>
 * </ul>
 * </p>
 *
 * @author Ghiyath & Sakariae
 * @version 1.0
 */
public class DirectoryTCPServer
{
    private int port;
    private static final Gson gson = new Gson();
    private RegistryService registry;
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private boolean running;

    /** Read timeout for client connections in milliseconds */
    private static final int READ_TIMEOUT_MS = 5000;

    /** Maximum allowed length for a single line of input */
    private static final int MAX_LINE_LEN = 2048;

    /**
     * Constructs a new DirectoryTCPServer.
     *
     * @param port the port number on which the server will listen for connections
     * @param registry the registry service to handle registration and updates
     */
    public DirectoryTCPServer(int port, RegistryService registry)
    {
        this.port = port;
        this.registry = registry;
    }

    /**
     * Starts the TCP server and begins accepting client connections.
     * <p>
     * Creates a server socket on the configured port and initializes a cached
     * thread pool for handling client connections concurrently.
     * </p>
     *
     * @throws RuntimeException if the server socket cannot be created or bound
     */
    public void start()
    {
        try{
            serverSocket = new ServerSocket(port);
            executor = Executors.newCachedThreadPool();
            running = true;
            new Thread(this::acceptLoop).start();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Server could not connect.." + e);
        }
    }

    /**
     * Stops the TCP server and shuts down all client connection threads.
     * <p>
     * This method closes the server socket and initiates an orderly shutdown
     * of the thread pool executor.
     * </p>
     */
    public void stop()
    {
        running = false;
        closeSocketCon(serverSocket);
        executor.shutdown();
    }

    /**
     * Main server loop that continuously accepts incoming client connections.
     * <p>
     * Each accepted connection is submitted to the executor service for
     * concurrent processing. This method runs until the server is stopped.
     * </p>
     */
    private void acceptLoop()
    {
        while(running)
        {
            try
            {
                Socket socket = serverSocket.accept();
                executor.submit(() -> {
                    handleClient(socket);
                });
            }
            catch (IOException e)
            {
                if(running)
                {
                    throw new RuntimeException("Accept failed!"+e);
                }
            }
        }
    }

    /**
     * Handles an individual client connection.
     * <p>
     * Reads a JSON command from the client, parses it, and executes the
     * appropriate registry operation (REGISTER, RENEW, or UPDATE). Sends
     * a JSON response containing the status code and TTL if successful.
     * </p>
     * <p>
     * Expected JSON format: {"CMD":"REGISTER|RENEW|UPDATE", "NAME":"serviceName", "IPv4":"ipAddress"}
     * </p>
     *
     * @param socket the client socket connection to handle
     */
    private void handleClient(Socket socket) {
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            socket.setSoTimeout(READ_TIMEOUT_MS);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            String line = readLineWithLimit(reader, MAX_LINE_LEN);
            System.out.println("TCP IN  " + line);
            if (line == null) { sendstatus(writer, StatusCodes.UNKNOWN_CMD); return; }

            Map<String,String> req = tryParseJsonMap(line);
            if (req == null)   { sendstatus(writer, StatusCodes.UNKNOWN_CMD); return; }

            String cmd  = req.get("CMD");
            String name = req.get("NAME");
            String ip   = (req.get("IPv4") != null) ? req.get("IPv4")
                    : (req.get("IP")   != null) ? req.get("IP")
                    : socket.getInetAddress().getHostAddress();

            if (cmd == null || name == null) { sendstatus(writer, StatusCodes.UNKNOWN_CMD); return; }
            cmd = cmd.toUpperCase(Locale.ROOT);

            long ttl;
            if ("REGISTER".equals(cmd)) {
                ttl = registry.register(name, ip);
                sendOkWithTtl(writer, ttl);
            } else if ("RENEW".equals(cmd) || "UPDATE".equals(cmd)) {
                ttl = registry.update(name, ip);
                sendOkWithTtl(writer, ttl);
            } else {
                sendstatus(writer, StatusCodes.UNKNOWN_CMD);
            }

        } catch (StatusExeption se) {
            safeSendStatus(writer, se.getCode());
        } catch (Exception e) {
            System.err.println("DirectoryTCPServer error: " + e);
            safeSendStatus(writer, StatusCodes.SERVER_ERROR);
        } finally {
            closeSocketCon(socket);
        }
    }

    /**
     * Safely sends a status code to the client, suppressing any IO exceptions.
     * <p>
     * This method is used in exception handlers where throwing another
     * exception would be counterproductive.
     * </p>
     *
     * @param w the BufferedWriter to write to (may be null)
     * @param code the status code to send
     */
    private void safeSendStatus(BufferedWriter w, String code) {
        if (w == null) return;
        try { sendstatus(w, code); } catch (IOException ignore) {}
    }

    /**
     * Sends a status code response to the client in JSON format.
     * <p>
     * Response format: {"STATUS":"statusCode"}
     * </p>
     *
     * @param writer the BufferedWriter to write the response to
     * @param code the status code to send
     * @throws IOException if an I/O error occurs while writing
     */
    private void sendstatus(BufferedWriter writer, String code) throws IOException
    {
        Map<String,String> payload = new HashMap<>();
        payload.put("STATUS",code);
        writeJsonLine(writer, payload);
    }

    /**
     * Sends a TTL (Time To Live) value to the client in JSON format.
     * <p>
     * Response format: {"TTL":"ttlValue"}
     * </p>
     *
     * @param writer the BufferedWriter to write the response to
     * @param ttlSec the TTL value in seconds
     * @throws IOException if an I/O error occurs while writing
     */
    private void sendTtl(BufferedWriter writer, long ttlSec) throws IOException
    {
        String ttlStr = format6(ttlSec);
        Map<String,String> map = new HashMap<>();
        map.put("TTL", ttlStr);

        writeJsonLine(writer, map);
    }

    /**
     * Sends a successful response with status OK and TTL value to the client.
     * <p>
     * Response format: {"STATUS":"OK", "TTL":"ttlValue"}
     * </p>
     *
     * @param writer the BufferedWriter to write the response to
     * @param ttlSec the TTL value in seconds
     * @throws IOException if an I/O error occurs while writing
     */
    private void sendOkWithTtl(BufferedWriter writer, long ttlSec)
            throws IOException
    {
        Map<String,String> map = new HashMap<>();
        map.put("STATUS",StatusCodes.OK);
        map.put("TTL", format6(ttlSec));
        writeJsonLine(writer,map);
    }
}