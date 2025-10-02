package via.vinylsystem.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class CatalogClient {
    private final String serverIp;
    private static final int SERVER_PORT = 6000;

    public CatalogClient(String serverIp) {
        this.serverIp = serverIp;
    }

    public String sendQuery(String query) throws Exception {
        try (Socket socket = new Socket(serverIp, SERVER_PORT);
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
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Enter server IP: ");
        String serverIp = reader.readLine().trim();


        try (Socket testSocket = new Socket(serverIp, SERVER_PORT)) {
            System.out.println("Connection successful to " + serverIp + ":" + SERVER_PORT);
        } catch (Exception e) {
            System.err.println("Failed to connect to " + serverIp + ":" + SERVER_PORT);
            return;
        }

        CatalogClient client = new CatalogClient(serverIp);
        System.out.println("Enter query (e.g., FIND ARTIST=Pink Floyd):");
        String query = reader.readLine();
        String response = client.sendQuery(query);
        System.out.println("Response:\n" + response);
    }
}