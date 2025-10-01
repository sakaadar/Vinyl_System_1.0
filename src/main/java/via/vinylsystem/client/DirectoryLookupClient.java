package via.vinylsystem.client;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Duration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DirectoryLookupClient {

  public static class LookupResponse {
    private final boolean ok;
    private final String ip;
    private final long ttlSeconds;
    private final String errorCode;

    private LookupResponse(boolean ok, String ip, long ttlSeconds, String errorCode) {
      this.ok = ok;
      this.ip = ip;
      this.ttlSeconds = ttlSeconds;
      this.errorCode = errorCode;
    }
    public boolean isOk() { return ok; }
    public String getIp() { return ip; }
    public long getTtlSeconds() { return ttlSeconds; }
    public String getErrorCode() { return errorCode; }
    public String toString() {
      return ok ? ("OK ip=" + ip + " ttl=" + ttlSeconds + "s")
                : ("ERROR code=" + errorCode);
    }
  }

  private final InetAddress directoryAddress;
  private final int directoryPort;
  private final int timeoutMillis;

  public DirectoryLookupClient(String host, int port) throws Exception {
    this(host, port, 2000);
  }

  public DirectoryLookupClient(String host, int port, int timeoutMillis) throws Exception {
    this.directoryAddress = InetAddress.getByName(host);
    this.directoryPort = port;
    this.timeoutMillis = timeoutMillis;
  }


  public LookupResponse lookup(String name) throws Exception {
    try (DatagramSocket socket = new DatagramSocket()) {
      socket.setSoTimeout(timeoutMillis);
      byte[] out = ("LOOKUP " + name).getBytes();
      DatagramPacket req = new DatagramPacket(out, out.length, directoryAddress, directoryPort);
      socket.send(req);

      byte[] buf = new byte[512];
      DatagramPacket resp = new DatagramPacket(buf, buf.length);
      socket.receive(resp);

      String line = new String(resp.getData(), 0, resp.getLength()).trim();
      ObjectMapper mapper = new ObjectMapper();
      JsonNode node = mapper.readTree(line);

      if (node.has("status") && "OK".equalsIgnoreCase(node.get("status").asText())) {
        String ip = node.has("ip") ? node.get("ip").asText() : null;
        long ttl = node.has("ttl") ? node.get("ttl").asLong() : 0;
        return new LookupResponse(true, ip, ttl, null);
      } else if (node.has("status") && "ERROR".equalsIgnoreCase(node.get("status").asText())) {
        String code = node.has("code") ? node.get("code").asText() : "000001";
        return new LookupResponse(false, null, 0, code);
      }
      return new LookupResponse(false, null, 0, "000001");
    }
  }
   /* you can use this to look up a server by ip instead of name, but it's not needed now.
  public LookupResponse lookupByIp(String ip) throws Exception {
    try (DatagramSocket socket = new DatagramSocket()) {
      socket.setSoTimeout(timeoutMillis);
      byte[] out = ("LOOKUP_IP " + ip).getBytes();
      DatagramPacket req = new DatagramPacket(out, out.length, directoryAddress, directoryPort);
      socket.send(req);

      byte[] buf = new byte[512];
      DatagramPacket resp = new DatagramPacket(buf, buf.length);
      socket.receive(resp);

      String line = new String(resp.getData(), 0, resp.getLength()).trim();
      ObjectMapper mapper = new ObjectMapper();
      JsonNode node = mapper.readTree(line);

      if (node.has("status") && "OK".equalsIgnoreCase(node.get("status").asText())) {
        String name = node.has("name") ? node.get("name").asText() : null;
        long ttl = node.has("ttl") ? node.get("ttl").asLong() : 0;
        // For symmetry, return name in the ip field
        return new LookupResponse(true, name, ttl, null);
      } else if (node.has("status") && "ERROR".equalsIgnoreCase(node.get("status").asText())) {
        String code = node.has("code") ? node.get("code").asText() : "000001";
        return new LookupResponse(false, null, 0, code);
      }
      return new LookupResponse(false, null, 0, "000001");
    }
  }
  */

  public static LookupResponse lookupByNameTcp(String directoryHost, int directoryPort, String serverName) throws Exception {
      try (java.net.Socket socket = new java.net.Socket(directoryHost, directoryPort);
           java.io.PrintWriter out = new java.io.PrintWriter(socket.getOutputStream(), true);
           java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()))) {
          out.println("LOOKUP " + serverName);
          String response = in.readLine();
          com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
          com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(response);

          if (node.has("status") && "OK".equalsIgnoreCase(node.get("status").asText())) {
              String ip = node.has("ip") ? node.get("ip").asText() : null;
              long ttl = node.has("ttl") ? node.get("ttl").asLong() : 0;
              return new LookupResponse(true, ip, ttl, null);
          } else if (node.has("status") && "ERROR".equalsIgnoreCase(node.get("status").asText())) {
              String code = node.has("code") ? node.get("code").asText() : "000001";
              return new LookupResponse(false, null, 0, code);
          }
          return new LookupResponse(false, null, 0, "000001");
      }
  }

public static void main(String[] args) throws Exception {
    LookupResponse byName = lookupByNameTcp("localhost", 5000, "vinyl.group1.pro2x");
    if (byName.isOk()) {
        System.out.println(byName.getIp());
    } else {
        System.out.println("Lookup failed: " + byName.getErrorCode());
    }
}


}