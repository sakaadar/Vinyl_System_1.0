package via.vinylsystem.directory;

import java.io.Closeable;

import via.vinylsystem.Model.Registration;
import via.vinylsystem.Util.JsonUtils;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static via.vinylsystem.Util.JsonUtils.format6;
import static via.vinylsystem.Util.JsonUtils.tryParseJsonMap;

/**
 * UDP server for handling directory service lookup requests.
 * <p>
 * This server accepts JSON-formatted lookup queries from clients to retrieve
 * registration information from the directory service. Clients can query by
 * service name or IP address to get registration details including TTL.
 * </p>
 * <p>
 * Supported queries:
 * <ul>
 *   <li>Lookup by NAME - Retrieve registration information using service name</li>
 *   <li>Lookup by IP - Retrieve registration information using IP address</li>
 * </ul>
 * </p>
 * <p>
 * Response format includes STATUS, NAME, IPv4, and TTL (time to live in seconds).
 * </p>
 *
 * @author Ghiyath & sakariae
 * @version 1.0
 */
public class DirectoryUDPServer
{
    private int port;
    private RegistryService registry;
    private DatagramSocket socket;
    private volatile boolean running;

    /** Maximum UDP packet size in bytes */
    private static final int MAX_UDP = 2048;

    /**
     * Constructs a new DirectoryUDPServer.
     *
     * @param port the port number on which the server will listen for UDP packets
     * @param registry the registry service to query for registration information
     */
    public DirectoryUDPServer(int port, RegistryService registry)
    {
        this.port = port;
        this.registry = registry;
    }

    /**
     * Starts the UDP server and begins listening for incoming packets.
     * <p>
     * Creates a datagram socket on the configured port and starts a receive
     * loop in a separate thread to handle incoming lookup requests.
     * </p>
     *
     * @throws RuntimeException if the datagram socket cannot be created or bound
     */
    public void start(){
        try
        {
            socket = new DatagramSocket(port);
            running = true;
            new Thread(this::receiveLoop).start();

        }
        catch (SocketException e)
        {
            throw new RuntimeException("Could not connect: "+e);
        }
    }

    /**
     * Stops the UDP server and closes the datagram socket.
     * <p>
     * This method stops the receive loop and releases the socket resources.
     * </p>
     */
    public void stop(){
        running = false;
        closeSocket(socket);
    }

    /**
     * Main server loop that continuously receives and processes UDP packets.
     * <p>
     * Each received packet is processed by the handlePacket method. The loop
     * continues until the server is stopped. Socket exceptions cause the loop
     * to terminate gracefully.
     * </p>
     */
    private void receiveLoop()
    {
        byte[] buffer = new byte[MAX_UDP];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while(running)
        {
            try
            {
                socket.receive(packet);
                handlePacket(packet);

                //reset packet length
                packet.setLength(buffer.length);
            }
            catch(SocketException e){
                break;
            }
            catch (IOException e)
            {
                System.out.println("UDP receive error"+e);
            }
        }
    }

    /**
     * Handles an individual UDP packet containing a lookup request.
     * <p>
     * Parses the JSON request from the packet, extracts the NAME or IP parameter,
     * queries the registry service, calculates remaining TTL, and sends a JSON
     * response back to the client.
     * </p>
     * <p>
     * Expected JSON format: {"NAME":"serviceName"} or {"IP":"ipAddress"}
     * </p>
     * <p>
     * Response format: {"STATUS":"statusCode", "NAME":"serviceName",
     * "IPv4":"ipAddress", "TTL":"ttlValue"}
     * </p>
     *
     * @param packet the datagram packet containing the lookup request
     * @throws RuntimeException if an unexpected error occurs during processing
     */
    private void handlePacket(DatagramPacket packet)
    {
        try{
            //læs antal bytes -> omdan til string -> også trim
            String text = new String(
                    packet.getData(),
                    packet.getOffset(),
                    packet.getLength(),
                    StandardCharsets.UTF_8
            ).trim();
            //Parse to json
            Map<String, String> req = tryParseJsonMap(text);
            if(req == null)
            {
                sendJson(packet.getAddress(),packet.getPort(),Map.of("STATUS: ",StatusCodes.UNKNOWN_CMD));
            }
            //Tjek name og ip
            boolean hasName = req.containsKey("NAME");
            boolean hasIp = req.containsKey("IP");

            if(!hasName && !hasIp)
            {
                sendJson(packet.getAddress(), packet.getPort(), Map.of("STATUS: ",StatusCodes.UNKNOWN_CMD));
            }
            //slå op i registry
            try{
                Registration reg;
                if(hasName){
                    String name = req.get("NAME");
                    if(name==null || name.isEmpty()){
                        sendJson(packet.getAddress(), packet.getPort(), Map.of("STATUS: ", StatusCodes.UNKNOWN_CMD));
                    }
                    reg = registry.findByName(name);
                }
                else{
                    String ipReq = req.get(("IP"));
                    if(ipReq==null || ipReq.isEmpty())
                    {
                        sendJson(packet.getAddress(), packet.getPort(), Map.of("STATUS: ", StatusCodes.UNKNOWN_CMD));
                    }
                    reg = registry.findByIp(ipReq);
                }
                //Hvis fundet beregn ttl og send svar
                long nowMs = System.currentTimeMillis();
                long ttlLeftSec = Math.max(0L,(reg.getExpiresAtMillis()- nowMs + 999) / 1000L);

                long ttlClamped = Math.min(999_999L,ttlLeftSec);
                Map<String,String> response = new HashMap<>();
                response.put("STATUS", "000000");
                response.put("NAME", reg.getName());
                response.put("IPv4", reg.getIp());
                response.put("TTL", format6(ttlClamped));
                sendJson(packet.getAddress(), packet.getPort(),response);
            }
            catch (StatusExeption e)
            {
                sendJson(packet.getAddress(), packet.getPort(),Map.of("STATUS: ", e.getCode()));
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends a JSON response back to the client via UDP.
     * <p>
     * Converts the payload map to JSON format, encodes it as UTF-8 bytes,
     * and sends it as a datagram packet to the specified address and port.
     * </p>
     *
     * @param address the destination IP address
     * @param port the destination port number
     * @param payload the map containing the response data to be converted to JSON
     * @throws RuntimeException if an I/O error occurs while sending the packet
     */
    private void sendJson(InetAddress address, int port, Map<String,String> payload)
    {
        try
        {
            String json = JsonUtils.toJson(payload);

            //transform JSON -> bytes
            byte[] data = json.getBytes(StandardCharsets.UTF_8);

            //Lav UDP-pakke
            DatagramPacket outPacket = new DatagramPacket(data, data.length, address, port);

            //Send via socket
            socket.send(outPacket);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Safely closes a Closeable resource, suppressing any IO exceptions.
     * <p>
     * This method is used to clean up resources without propagating
     * exceptions during shutdown.
     * </p>
     *
     * @param c the Closeable resource to close (may be null)
     */
    private void closeSocket(Closeable c)
    {
        try{
            if(c != null)
            {
                c.close();
            }
        }
        catch (IOException e)
        {
            //Ignore
        }
    }
}