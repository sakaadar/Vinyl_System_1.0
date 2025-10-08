package via.vinylsystem.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * UDP client for resolving Vinyl server IPs via the directory service.
 * <p>
 * Sends a JSON request containing the server name to the directory server over UDP
 * and receives the corresponding IP address and TTL. Handles parsing of the
 * directory's JSON response and converts it into a {@link Resolve} record.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * DirectoryClient dir = new DirectoryClient("127.0.0.1", 4555);
 * DirectoryClient.Resolve res = dir.resolveByName("Happy_music.group3.pro2");
 * System.out.println(res.ip() + " TTL=" + res.ttlSec());
 * </pre>
 * </p>
 *
 * @author Ghiyath & sakariae
 * @version 1.0
 */
public class DirectoryClient
{
    /** Directory server address. */
    private final InetAddress dirAddr;

    /** Directory server UDP port. */
    private final int dirPort;

    /**
     * Record representing the resolved server information.
     * <p>
     * Contains the server name, IPv4 address, and TTL in seconds.
     * </p>
     */
    public static record Resolve(String name, String ip, int ttlSec){}

    /**
     * Constructs a new DirectoryClient.
     *
     * @param directoryHost hostname or IP of the directory server
     * @param directoryUdpPort UDP port of the directory server
     * @throws UnknownHostException if the hostname cannot be resolved
     */
    public DirectoryClient(String directoryHost, int directoryUdpPort)
            throws UnknownHostException
    {
        this.dirAddr = InetAddress.getByName(directoryHost);
        this.dirPort = directoryUdpPort;
    }

    /**
     * Resolves a server name to its IP address and TTL via the directory server.
     * <p>
     * Sends a JSON request over UDP and expects a JSON response containing
     * the server's IP, TTL, and confirmed name. Throws an exception if the
     * directory responds with an error or no IP is provided.
     * </p>
     *
     * @param name the server name to resolve
     * @return a {@link Resolve} record containing the resolved server info
     * @throws RuntimeException if a socket or I/O error occurs
     */
    public Resolve resolveByName(String name){
        try(DatagramSocket socket = new DatagramSocket()){
            socket.setSoTimeout(1500);

            // Send request to directory server
            String reqJson = "{\"NAME\":\"" + name + "\"}";
            byte[] data = reqJson.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(data, data.length, dirAddr, dirPort));

            // Receive response
            byte[] buf = new byte[2048];
            DatagramPacket resp = new DatagramPacket(buf, buf.length);
            socket.receive(resp);

            String json = new String(resp.getData(), resp.getOffset(), resp.getLength(), StandardCharsets.UTF_8);
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();

            // Parse response
            if(object.has("IPv4")){
                String ip = object.get("IPv4").getAsString();
                int ttlSec = Integer.parseInt(object.get("TTL").getAsString()); // "000900" -> 900
                String outName = object.get("NAME").getAsString();
                return new Resolve(outName, ip, ttlSec);
            } else {
                String status = object.has("STATUS") ? object.get("STATUS").getAsString() : "??????";
                throw new IOException("Directory error STATUS=" + status);
            }
        }
        catch (SocketException e)
        {
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
