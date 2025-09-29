package via.vinylsystem.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class CatalogClient {
  private static final String SERVER_IP = "127.0.0.1";
  private static final int SERVER_PORT = 6000;

  public String sendQuery(String query) throws Exception {
    try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

      out.println(query);

      StringBuilder response = new StringBuilder();
      String line;
      while ((line = in.readLine()) != null) {
        response.append(line).append(System.lineSeparator());
      }
      return response.toString().trim();
    }
  }

    public static void main(String[] args) throws Exception {
        CatalogClient client = new CatalogClient();
        System.out.println("Enter query (e.g., FIND ARTIST=Pink Floyd):");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String query = reader.readLine();
        System.out.println("client is send query: " + query);
        String response = client.sendQuery(query);
        System.out.println("Response:\n" + response);
    }

}