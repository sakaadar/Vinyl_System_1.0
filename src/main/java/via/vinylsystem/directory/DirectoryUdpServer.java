package via.vinylsystem.directory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

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

  private String handle(String req) {
    String[] parts = req.split("\\s+", 2);
    if (parts.length == 2) {
      String cmd = parts[0].toUpperCase();
      String arg = parts[1];
      switch (cmd) {
        case "REGISTER":
          try {
            long ttl = registryService.register(arg, "0.0.0.0");
            return "OK " + ttl;
          } catch (StatusExeption e) {
            return "ERROR " + e.getCode();
          }
        case "UPDATE":
          try {
            long ttl = registryService.update(arg, "0.0.0.0");
            return "OK " + ttl;
          } catch (StatusExeption e) {
            return "ERROR " + e.getCode();
          }
        case "LOOKUP":
          try {
            LookUpResult res = registryService.lookup(arg);
            return "OK " + res.getIp() + " " + res.getTtlSeconds();
          } catch (StatusExeption e) {
            return "ERROR " + e.getCode();
          }
        case "LOOKUP_IP":
          try {
            LookUpResult res = registryService.lookupByIp(arg);
            return "OK " + res.getName() + " " + res.getTtlSeconds();
          } catch (StatusExeption e) {
            return "ERROR " + e.getCode();
          }
      }
    }
    return "ERROR " + StatusCodes.UNKNOWN_CMD;
  }

  public void shutdown() {
    running = false;
    if (socket != null) socket.close();
  }
}