package via.vinylsystem.client;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class ClientApp
{
  public static void main(String[] args) throws IOException
  {
    String directoryHost = System.getenv().getOrDefault("VINYL_DIR_HOST", "127.0.0.1");
    int directoryUdp = Integer.parseInt(System.getenv().getOrDefault("VINYL_DIR_UDP", "4555"));
    int VinylTcpPort = Integer.parseInt(System.getenv().getOrDefault("VINYL_PORT", "5044"));

    DirectoryClient dir = new DirectoryClient(directoryHost,directoryUdp);
    VinylServerClient conn = null;
    Scanner sc = new Scanner(System.in);

    while(true)
    {
      System.out.println("\nCmd [CONNECT/LIST/SEARCH/GET/QUIT]: ");
      String cmd = sc.next().trim().toUpperCase();

      switch (cmd) {
        case "CONNECT":
          System.out.println("Server name: ");
          String name = sc.next();
          DirectoryClient.Resolve res = dir.resolveByName(name);
          System.out.println("Resolved: " + res.name() + " ->" + res.ip() + " (TTL " + res.ttlSec() + "s)");
          if(conn != null) conn.close();
          conn = new VinylServerClient(res.ip(), VinylTcpPort);
          System.out.println("TCP Connected");
        case "LIST":
          ensure(conn);
          System.out.println(conn.list());
        case "SEARCH":
          ensure(conn);
          System.out.println("Query: ");
          sc.nextLine();
          String q = sc.nextLine();
          System.out.println(conn.search(q));
        case "GET":
          ensure(conn);
          System.out.println("ID: ");
          String id = sc.next();
          System.out.println(conn.get(id));
        case "QUIT":
          if(conn != null)
          {
            conn.close();
            return;
          }
        default: System.out.println("Uknowkn command..");
      }

    }
  }
  private static void ensure(VinylServerClient c)
  {
    if(c == null) throw new IllegalStateException("Brug CONNECT først.");
  }
}
