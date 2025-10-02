package via.vinylsystem.directory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static via.vinylsystem.Util.JsonUtils.*;

public class DirectoryTCPServer
{
  private int port;
  private static final Gson gson = new Gson(); //Konvertere til JSON
  private RegistryService registry;
  private ServerSocket serverSocket;
  private ExecutorService executor;
  private boolean running;

  private static final int READ_TIMEOUT_MS = 5000;
  private static final int  MAX_LINE_LEN = 2048;

  public DirectoryTCPServer(int port, RegistryService registry)
  {
    this.port = port;
    this.registry = registry;
  }

  public void start()
  {
    try{
      serverSocket = new ServerSocket(port);
      executor = Executors.newCachedThreadPool();
      running = true;
      new Thread(this::acceptLoop).start();
    }
    catch (IOException e)
    {
      throw new RuntimeException("Server could not connect.." + e);
    }
  }
  public void stop()
  {
    running = false;
    closeSocketCon(serverSocket);
    executor.shutdown();
  }

  private void acceptLoop()
  {
    while(running)
    {
      try
      {
        Socket socket = serverSocket.accept();
        executor.submit(() -> {
          handleClient(socket);
        });
      }
      catch (IOException e)
      {
        if(running)
        {
          throw new RuntimeException("Accept failed!"+e);
        }
      }
    }
  }

  private void handleClient(Socket socket) {
    BufferedReader reader = null;
    BufferedWriter writer = null;
    try {
      socket.setSoTimeout(READ_TIMEOUT_MS);
      reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
      writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

      String line = readLineWithLimit(reader, MAX_LINE_LEN);
      System.out.println("TCP IN  " + line);
      if (line == null) { sendstatus(writer, StatusCodes.UNKNOWN_CMD); return; }

      Map<String,String> req = tryParseJsonMap(line);
      if (req == null)   { sendstatus(writer, StatusCodes.UNKNOWN_CMD); return; }

      String cmd  = req.get("CMD");
      String name = req.get("NAME");
      String ip   = (req.get("IPv4") != null) ? req.get("IPv4")
          : (req.get("IP")   != null) ? req.get("IP")
          : socket.getInetAddress().getHostAddress();

      if (cmd == null || name == null) { sendstatus(writer, StatusCodes.UNKNOWN_CMD); return; }
      cmd = cmd.toUpperCase(Locale.ROOT);

      long ttl;
      if ("REGISTER".equals(cmd)) {
        ttl = registry.register(name, ip);
        sendOkWithTtl(writer, ttl);
      } else if ("RENEW".equals(cmd) || "UPDATE".equals(cmd)) {
        ttl = registry.update(name, ip);
        sendOkWithTtl(writer, ttl);
      } else {
        sendstatus(writer, StatusCodes.UNKNOWN_CMD);
      }

    } catch (StatusExeption se) {
      safeSendStatus(writer, se.getCode());
    } catch (Exception e) {                         // ← fang ALT
      System.err.println("DirectoryTCPServer error: " + e);
      safeSendStatus(writer, StatusCodes.SERVER_ERROR); // "000500"
    } finally {
      closeSocketCon(socket);
    }
  }

  private void safeSendStatus(BufferedWriter w, String code) {
    if (w == null) return;
    try { sendstatus(w, code); } catch (IOException ignore) {}
  }


  private void sendstatus(BufferedWriter writer, String code) throws IOException
  {
    Map<String,String> payload = new HashMap<>();
    payload.put("STATUS",code);
   writeJsonLine(writer, payload);
  }

  private void sendTtl(BufferedWriter writer,  long ttlSec) throws IOException
  {
    String ttlStr = format6(ttlSec);
    Map<String,String> map = new HashMap<>();
    map.put("TTL", ttlStr);

    writeJsonLine(writer, map);
  }

  private void sendOkWithTtl(BufferedWriter writer,  long ttlSec)
      throws IOException
  {
    Map<String,String> map = new HashMap<>();
    map.put("STATUS",StatusCodes.OK);
    map.put("TTL", format6(ttlSec));
    writeJsonLine(writer,map);
  }

}
