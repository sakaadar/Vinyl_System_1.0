package via.vinylsystem.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class RegisterVinylServer {
  public static void main(String[] args) throws Exception {
    String host = "localhost";
    String name = "vinyl.group1.pro2x"; // Hardcoded name
    try (Socket s = new Socket(host, 5000);
         PrintWriter out = new PrintWriter(s.getOutputStream(), true);
         BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
      out.println("REGISTER " + name);
      String line = in.readLine();
      System.out.println("REGISTER response: " + line);
    }
  }
}