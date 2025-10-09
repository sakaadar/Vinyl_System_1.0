package via.vinylsystem.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import via.vinylsystem.Model.Track;
import via.vinylsystem.Util.JsonUtils;
import via.vinylsystem.Util.yamlLoader;
import via.vinylsystem.directory.RegistryService;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main entry point for the Vinyl System server.
 * <p>
 * This class initializes and starts a TCP catalog server, registers it with the
 * central directory, and maintains the directory registration with periodic
 * TTL-based renewals.
 * </p>
 * <p>
 * The server configuration can be set using command-line arguments or environment
 * variables:
 * <ul>
 *   <li>VINNYL_NAME - Name of the server (default: "Happy_music.group3.pro2")</li>
 *   <li>VINYL_PORT - TCP port for the catalog server (default: 7070)</li>
 *   <li>VINYL_DIR_HOST - Directory server host (default: 127.0.0.1)</li>
 *   <li>VINYL_DIR_TCP - Directory server port (default: 5044)</li>
 *   <li>VINYL_TTL - Time-to-live for directory registration in seconds (default: 900)</li>
 *   <li>VINYL_IP - IP address of this server (default: 127.0.0.1)</li>
 * </ul>
 * </p>
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Start the TCP catalog server using {@link CatalogServer}.</li>
 *   <li>Register the server with the directory.</li>
 *   <li>Periodically renew registration to prevent TTL expiration.</li>
 * </ul>
 * </p>
 *
 * @author Ghiyath & sakariae
 * @version 1.0
 */
public class Servermain
{
    /**
     * Main entry point of the server application.
     * <p>
     * Parses configuration from command-line arguments or environment variables,
     * starts the catalog server, registers with the directory, and schedules
     * periodic renewals.
     * </p>
     *
     * @param args optional command-line arguments: args[0] = server name
     * @throws IOException if the catalog server fails to start
     * @throws IllegalArgumentException if the provided server name is invalid
     */
    public static void main(String[] args) throws IOException
    {
        Map<String, Object> config;
        try {
            config = yamlLoader.loadConfig("server_reg_contract.yaml");
        } catch (Exception e) {
            throw new IOException("Failed to load YAML config: " + e.getMessage(), e);
        }
        Map<String, Object> serverConfig = (Map<String, Object>) config.get("server");

        String serverName = getArg(args, 0, (String) serverConfig.get("server_name"));
        int servicePort = 7070; // If you want to add to YAML, use: (int) serverConfig.get("port")
        String directoryHost = (String) serverConfig.get("dir_ip");
        int directoryTcp = (int) serverConfig.get("dir_tcp_port");
        int ttlSec = (int) serverConfig.get("ttl");
        String ip = (String) serverConfig.get("ip");

        if(!RegistryService.validName(serverName)) throw new IllegalArgumentException("Ugyldigt navn: " + serverName);

        // Start TCP catalog server
        List<Track> catalog = CatalogServer.seedCatalog();
        CatalogServer srv = new CatalogServer(servicePort, catalog);
        srv.start();


        // Register server with directory
        sendToDirectory("REGISTER",serverName,ip,ttlSec,directoryHost,directoryTcp);

        // Schedule periodic TTL renewals (every TTL/2 seconds)
        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
        ses.scheduleAtFixedRate(() -> {
            try {
                sendToDirectory("RENEW",serverName,ip,ttlSec,directoryHost,directoryTcp);
            } catch (Exception e) {
                System.err.println("RENEW Failed: " + e.getMessage());
            }
        }, ttlSec / 2, ttlSec / 2, TimeUnit.SECONDS);
    }

    /**
     * Sends a command message to the directory server.
     * <p>
     * Supported commands include:
     * <ul>
     *   <li>REGISTER - Register the server with the directory</li>
     *   <li>RENEW - Renew the server's TTL registration</li>
     * </ul>
     * </p>
     *
     * @param cmd the command to send ("REGISTER" or "RENEW")
     * @param name the server name
     * @param ip the server IP address
     * @param ttlSec time-to-live in seconds
     * @param host directory server host
     * @param port directory server TCP port
     */
    private static void sendToDirectory(String cmd, String name, String ip, int ttlSec,
                                        String host, int port)
    {
        JsonObject msg = new JsonObject();
        msg.addProperty("CMD", cmd);
        msg.addProperty("NAME", name);
        msg.addProperty("IPv4", ip);
        msg.addProperty("TTL", JsonUtils.ttl6(ttlSec));

        try(Socket s = new Socket())
        {
            s.connect(new InetSocketAddress(host,port),2000);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(),
                    StandardCharsets.UTF_8));
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));

            out.write(msg.toString());
            out.write("\n");
            out.flush();

            String line = in.readLine();
            if(line == null) throw new EOFException("Directory lukkede forbindelsen");
            JsonObject resp = JsonParser.parseString(line).getAsJsonObject();
            String status = resp.has("STATUS") ? resp.get("STATUS").getAsString() : "??????";
            if(!"000000".equals(status))
            {
                throw new IOException("Directory STATUS=" + status + " for " + cmd);
            }
        }
        catch (IOException e)
        {
            System.out.println("Exception in Server starting: " + e.getMessage());
        }
    }

    /**
     * Retrieves an argument from the command-line arguments array.
     * <p>
     * If the argument at the specified index is missing or blank, returns
     * the provided default value.
     * </p>
     *
     * @param args the command-line arguments array
     * @param idx the index of the argument to retrieve
     * @param def the default value to return if argument is missing or blank
     * @return the argument value, or the default value if not provided
     */
    private static String getArg(String[] args, int idx, String def)
    {
        return (args != null && args.length > idx && args[idx] != null && !args[idx].isBlank()) ? args[idx] : def;
    }
}
