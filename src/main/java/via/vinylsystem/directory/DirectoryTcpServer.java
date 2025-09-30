package via.vinylsystem.directory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

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

    private String handle(String req) {
      String[] parts = req.split("\\s+", 2);
      if (parts.length == 2) {
        String cmd = parts[0].toUpperCase();
        String arg = parts[1];
        switch (cmd) {
          case "REGISTER":
            try {
              long ttl = registryService.register(arg, socket.getInetAddress().getHostAddress());
              return "OK " + ttl;
            } catch (StatusExeption e) {
              return "ERROR " + e.getCode();
            }
          case "UPDATE":
            try {
              long ttl = registryService.update(arg, socket.getInetAddress().getHostAddress());
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
  }
}