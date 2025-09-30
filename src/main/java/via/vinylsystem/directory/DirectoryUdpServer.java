package via.vinylsystem.directory;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

public class DirectoryUdpServer extends Thread {
  private final int port;
  private final RegistryService registryService;
  private DatagramSocket socket;
  private volatile boolean running;

  public DirectoryUdpServer(int port, RegistryService registryService) {
    this.port = port;
    this.registryService = registryService;
  }

  @Override
  public void run() {
    try {
      socket = new DatagramSocket(port);
      running = true;
      while (running) {
        try {
          byte[] buf = new byte[512];
          DatagramPacket pkt = new DatagramPacket(buf, buf.length);
            socket.receive(pkt);
          String request = new String(pkt.getData(), 0, pkt.getLength()).trim();
          String response = handle(request);
          byte[] out = response.getBytes();
          socket.send(new DatagramPacket(out, out.length, pkt.getAddress(), pkt.getPort()));
        } catch (IOException e) {
          if (running) System.err.println("UDP IO: " + e.getMessage());
        }
      }
    } catch (SocketException e) {
      System.err.println("UDP start failed: " + e.getMessage());
    } finally {
      if (socket != null && !socket.isClosed()) socket.close();
    }
  }

  private static final ObjectMapper mapper = new ObjectMapper();

  private String handle(String req) {
    String[] parts = req.split("\\s+", 2);
    Map<String, Object> json = new HashMap<>();
    if (parts.length == 2) {
      String cmd = parts[0].toUpperCase();
      String arg = parts[1];
      try {
        switch (cmd) {
          case "REGISTER":
            long ttl = registryService.register(arg, "0.0.0.0");
            json.put("status", "OK");
            json.put("ttl", ttl);
            break;
          case "UPDATE":
            ttl = registryService.update(arg, "0.0.0.0");
            json.put("status", "OK");
            json.put("ttl", ttl);
            break;
          case "LOOKUP":
            LookUpResult res = registryService.lookup(arg);
            json.put("status", "OK");
            json.put("ip", res.getIp());
            json.put("ttl", res.getTtlSeconds());
            break;
          case "LOOKUP_IP":
            res = registryService.lookupByIp(arg);
            json.put("status", "OK");
            json.put("name", res.getName());
            json.put("ttl", res.getTtlSeconds());
            break;
          default:
            json.put("status", "ERROR");
            json.put("code", StatusCodes.UNKNOWN_CMD);
        }
      } catch (StatusExeption e) {
        json.put("status", "ERROR");
        json.put("code", e.getCode());
      }
    } else {
      json.put("status", "ERROR");
      json.put("code", StatusCodes.UNKNOWN_CMD);
    }
    try {
      return mapper.writeValueAsString(json);
    } catch (Exception e) {
      return "{\"status\":\"ERROR\",\"code\":\"json_error\"}";
    }
  }
}