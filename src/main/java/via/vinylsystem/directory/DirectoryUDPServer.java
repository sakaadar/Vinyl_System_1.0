package via.vinylsystem.directory;

import java.io.Closeable;

import via.vinylsystem.Model.Registration;
import via.vinylsystem.Model.RegistryEvent;
import via.vinylsystem.Model.RegistryEventType;
import via.vinylsystem.Util.AuditLog;
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
    private final AuditLog audit;

    /** Maximum UDP packet size in bytes */
    private static final int MAX_UDP = 2048;

    /**
     * Constructs a new DirectoryUDPServer.
     *
     * @param port the port number on which the server will listen for UDP packets
     * @param registry the registry service to query for registration information
     */
    public DirectoryUDPServer(int port, RegistryService registry, AuditLog audit)
    {
        this.port = port;
        this.registry = registry;
        this.audit = audit;
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
    {   long nowMs = System.currentTimeMillis();
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
                sendJson(packet.getAddress(),packet.getPort(),Map.of("STATUS: ",StatusCodes.BAD_REQUEST));
                if(audit != null) audit.append(new RegistryEvent(nowMs, RegistryEventType.INVALIDATE,null,null,null, "UDP", "bad json"));
                return;
            }
            //Tjek name og ip
            boolean hasName = req.containsKey("NAME");
            boolean hasIp = req.containsKey("IP");

            if(!hasName && !hasIp)
            {
                sendJson(packet.getAddress(), packet.getPort(), Map.of("STATUS: ",StatusCodes.UNKNOWN_CMD));
                if(audit != null) audit.append(new RegistryEvent(nowMs, RegistryEventType.INVALIDATE,null,null,null,"UDP", "missing NAME or IP"));
                return;
            }
            String requestedName = hasName ? req.get("NAME") : null;
            String requestedIp   = hasIp   ? req.get("IP")   : null;
            if (hasName && (requestedName == null || requestedName.isEmpty())) {
                sendJson(packet.getAddress(), packet.getPort(), Map.of("STATUS", StatusCodes.BAD_REQUEST));
                if (audit != null) audit.append(new RegistryEvent(nowMs, RegistryEventType.INVALIDATE, null, null, null, "UDP", "empty NAME"));
                return;
            }
            if (hasIp && (requestedIp == null || requestedIp.isEmpty())) {
                sendJson(packet.getAddress(), packet.getPort(), Map.of("STATUS", StatusCodes.BAD_REQUEST));
                if (audit != null) audit.append(new RegistryEvent(nowMs, RegistryEventType.INVALIDATE, null, null, null, "UDP", "empty IP"));
                return;
            }
          Registration reg = hasName ? registry.findByName(requestedName) : registry.findByIp(requestedIp);

          if(reg != null){
              long ttlLeftSec = Math.max(0L, (reg.getExpiresAtMillis() - nowMs + 999) / 1000L); //runder op
              long ttlClamped = Math.min(999_999L, ttlLeftSec);

              //LOG: FOUND
              if(audit != null){
                  audit.append(new RegistryEvent(nowMs, RegistryEventType.LOOKUP,
                                                  reg.getName(),reg.getIp(),ttlClamped, "UDP", "FOUND"));
              }

              //Response
              Map<String,String> resp = new HashMap<>();
              resp.put("STATUS","000000");
              resp.put("NAME", reg.getName());
              resp.put("IPv4",reg.getIp());
              resp.put("TTL",format6(ttlClamped));
              sendJson(packet.getAddress(),packet.getPort(),resp);
              return;
          }

          //NOT FOUND
          if(audit!=null){
              String n = hasName ? requestedName : null;
              String ip = hasIp ? requestedIp : null;
              audit.append(new RegistryEvent(nowMs, RegistryEventType.LOOKUP,n,ip,0L,"UDP","NOT_FOUND"));
          }
          sendJson(packet.getAddress(),packet.getPort(),Map.of("STATUS",StatusCodes.NOT_FOUND));
          return;
        }
        catch (Exception e)
        {
            if (audit != null) {
                audit.append(new RegistryEvent(nowMs, RegistryEventType.ERROR, null, null, null, "UDP",
                    "exception: " + e.getClass().getSimpleName() + " " + e.getMessage()));
            }
            sendJson(packet.getAddress(), packet.getPort(), Map.of("STATUS", StatusCodes.SERVER_ERROR));
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