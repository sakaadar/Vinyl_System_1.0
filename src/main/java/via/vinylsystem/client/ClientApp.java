package via.vinylsystem.client;

import java.util.Locale;
import java.util.Scanner;

public class ClientApp {

  public static void main(String[] args) throws Exception {
    Scanner sc = new Scanner(System.in);

    String directoryHost = System.getenv().getOrDefault("VINYL_DIR_HOST", "127.0.0.1");
    int    directoryUdp  = Integer.parseInt(System.getenv().getOrDefault("VINYL_DIR_UDP", "4555"));
    int    vinylTcpPort  = Integer.parseInt(System.getenv().getOrDefault("VINYL_PORT", "7070"));

    DirectoryClient dir = new DirectoryClient(directoryHost, directoryUdp);
    VinylServerClient conn = null;

    System.out.println("Vinyl client");
    System.out.println("Commands: CONNECT [name] | LIST | SEARCH <q> | GET <id> | CLOSE | QUIT | HELP");

    while (true) {
      System.out.print("\n> ");
      String line = sc.nextLine().trim();
      if (line.isEmpty()) continue;

      String[] parts = line.split("\\s+", 2);
      String cmd = parts[0].toUpperCase(Locale.ROOT);
      String arg = (parts.length > 1) ? parts[1] : "";

      try {
        switch (cmd) {
          case "HELP" -> {
            System.out.println("CONNECT [name]  – resolve via directory and open TCP");
            System.out.println("LIST            – list tracks");
            System.out.println("SEARCH <q>      – search tracks");
            System.out.println("GET <id>        – get one track");
            System.out.println("CLOSE           – close TCP connection");
            System.out.println("QUIT            – exit client");
          }

          case "CONNECT" -> {
            String name = arg;
            if (name.isBlank()) {
              System.out.print("Server name: ");
              name = sc.nextLine().trim();
            }
            var res = dir.resolveByName(name);
            System.out.println("Resolved: " + res.name() + " -> " + res.ip() + " (TTL " + res.ttlSec() + "s)");
            if (conn != null) conn.close();
            conn = new VinylServerClient(res.ip(), vinylTcpPort);
            System.out.println("TCP connected.");
          }

          case "LIST" -> {
            ensureConnected(conn);
            System.out.println(conn.list());
          }

          case "SEARCH" -> {
            ensureConnected(conn);
            String q = arg;
            if (q.isBlank()) {
              System.out.print("Query: ");
              q = sc.nextLine().trim();
            }
            System.out.println(conn.search(q));
          }

          case "GET" -> {
            ensureConnected(conn);
            String id = arg;
            if (!id.isBlank()) {
              System.out.print("ID: ");
              id = sc.nextLine().trim();
            }
            System.out.println(conn.get(id));
          }

          case "CLOSE" -> {
            if (conn != null) { conn.close(); conn = null; System.out.println("TCP closed."); }
            else System.out.println("(not connected)");
          }

          case "QUIT", "EXIT" -> {
            if (conn != null) conn.close();
            return;
          }

          default -> System.out.println("Unknown command. Type HELP.");
        }
      } catch (IllegalStateException ise) {
        System.out.println(ise.getMessage());
      } catch (Exception e) {
        System.out.println("Error: " + e.getMessage());
      }
    }
  }

  private static void ensureConnected(VinylServerClient conn) {
    if (conn == null) throw new IllegalStateException("Not connected. Use CONNECT [name] first.");
  }
}
