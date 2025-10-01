// src/main/java/via/vinylsystem/server/CatalogServer.java
package via.vinylsystem.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class CatalogServer {

    private final VinylCatalog catalog = new VinylCatalog();


    private void registerInDirectory(String directoryHost, int directoryPort, String serverName) {
        try (Socket s = new Socket(directoryHost, directoryPort);
             PrintWriter out = new PrintWriter(s.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            out.println("REGISTER " + serverName + " " + s.getLocalAddress().getHostAddress() + " 6000");
            String response = in.readLine();
            System.out.println("Directory REGISTER response: " + response);
        } catch (IOException e) {
            System.err.println("Failed to register in directory: " + e.getMessage());
        }
    }

    public void start(int port) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("CatalogServer started on port " + port);
            while (true) {
                Socket client = serverSocket.accept();
                new Thread(() -> handleClient(client)).start();
            }
        }
    }

    private void handleClient(Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {
            String line = in.readLine();
            if (line != null) out.println(processRequest(line));
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            try { client.close(); } catch (IOException ignore) {}
        }
    }

    private String processRequest(String req) {
        if (req.startsWith("FIND ")) {
            String query = req.substring(5).trim();
            String artist = null, title = null;
            for (String part : query.split(";")) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2) {
                    if (kv[0].equalsIgnoreCase("ARTIST")) artist = kv[1];
                    if (kv[0].equalsIgnoreCase("TITLE")) title = kv[1];
                }
            }
            List<VinylRecord> result = catalog.search(artist, title);
            if (result.isEmpty()) return "NO MATCH";
            return result.stream().map(VinylRecord::toString).collect(java.util.stream.Collectors.joining("\n"));
        }
        return "ERROR UNKNOWN_CMD";
    }

    public static void main(String[] args) throws IOException {
        String directoryHost = "localhost";
        int directoryPort = 5000;
        String serverName = "vinyl.group1.pro2x";
        CatalogServer server = new CatalogServer();
        server.registerInDirectory(directoryHost, directoryPort, serverName);
        server.start(6000);
    }
}