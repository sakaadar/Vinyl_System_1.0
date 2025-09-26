package via.vinylsystem.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Registers a server name over TCP so the Directory stores the correct source IP.
 */
public class RegisterVinylServer {
  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.out.println("Usage: RegisterVinylServer <directoryHost> <name>");
      System.out.println("Example: RegisterVinylServer localhost vinyl.group1.pro2x");
      return;
    }
    String host = args[0];
    String name = args[1];
    try (Socket s = new Socket(host, 5000);
         PrintWriter out = new PrintWriter(s.getOutputStream(), true);
         BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
      out.println("REGISTER " + name);
      String line = in.readLine();
      System.out.println("REGISTER response: " + line);
    }
  }
}