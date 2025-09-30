package via.vinylsystem.directory;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

public class DirectoryTcpServer extends Thread {
  private final int port;
  private final RegistryService registryService;
  private ServerSocket serverSocket;
  private volatile boolean running;

  public DirectoryTcpServer(int port, RegistryService registryService) {
    this.port = port;
    this.registryService = registryService;
  }

  @Override
  public void run() {
    try {
      serverSocket = new ServerSocket(port);
      running = true;
      while (running) {
        try {
          Socket client = serverSocket.accept();
          new ClientHandler(client, registryService).start();
        } catch (IOException e) {
          if (running) System.err.println("Accept: " + e.getMessage());
        }
      }
    } catch (IOException e) {
      System.err.println("TCP start failed: " + e.getMessage());
    } finally {
      shutdown();
    }
  }

  public void shutdown() {
    running = false;
    try {
      if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
    } catch (IOException ignore) {}
  }

  private static class ClientHandler extends Thread {
    private final Socket socket;
    private final RegistryService registryService;

    ClientHandler(Socket socket, RegistryService registryService) {
      this.socket = socket;
      this.registryService = registryService;
    }

    @Override
    public void run() {
      try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
           PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
        String req;
        while ((req = in.readLine()) != null) {
          out.println(handle(req));
        }
      } catch (IOException e) {
        System.err.println("Client handler: " + e.getMessage());
      } finally {
        try { socket.close(); } catch (IOException ignore) {}
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
              long ttl = registryService.register(arg, socket.getInetAddress().getHostAddress());
              json.put("status", "OK");
              json.put("ttl", ttl);
              break;
            case "UPDATE":
              ttl = registryService.update(arg, socket.getInetAddress().getHostAddress());
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
}