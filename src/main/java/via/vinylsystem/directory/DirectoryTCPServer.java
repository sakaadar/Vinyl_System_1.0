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
          try
          {
            handleClient(socket);
          }
          catch (IOException e)
          {
            throw new RuntimeException(e);
          }
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

  private void handleClient(Socket socket) throws IOException
  {
    BufferedReader reader = null;
    BufferedWriter writer = null;
    try
    {
      socket.setSoTimeout(READ_TIMEOUT_MS);
      //Reader/writer
      reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
      writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

      String line = readLineWithLimit(reader, MAX_LINE_LEN);
      System.out.println("TCP IN  " + line);
      if(line == null)
      {
        sendstatus(writer,StatusCodes.UNKNOWN_CMD);
        return;
      }
      //Parse JSON -> Map
      Map<String, String> req = tryParseJsonMap(line);
      if(req == null){
        sendstatus(writer,StatusCodes.UNKNOWN_CMD);
        return;
      }
      //Extract fields + IP
      String cmd = req.get("CMD");
      String name = req.get("NAME");
      String ip;
      if(req.get("IPv4") != null){
        ip = req.get("IPv4");
      } else if(req.get("IP") != null){
        ip = req.get("IP");
      } else{
        ip = socket.getInetAddress().getHostAddress();
      }

      if(cmd == null || name == null){
        sendstatus(writer, StatusCodes.UNKNOWN_CMD);
        return;
      }
      cmd = cmd.toUpperCase(Locale.ROOT);

      //Dispatch to service
      try{
        long ttl;
        if("REGISTER".equals(cmd)){
         ttl = registry.register(name,ip);
          System.out.println("TCP OUT ");
          sendOkWithTtl(writer,ttl);
        }
        else if ("RENEW".equals(cmd)||"UPDATE".equals(cmd))
        {
          ttl = registry.update(name,ip);
          sendOkWithTtl(writer,ttl);
        }
        else{
          sendstatus(writer, StatusCodes.UNKNOWN_CMD);
        }
      }
      catch (StatusExeption se)
      {
        sendstatus(writer, se.getCode());
      }
    }
    catch (IOException e)
    {
      System.err.println("DirectoryTCPServer handle error: " + e.getMessage());
      sendstatus(writer, StatusCodes.SERVER_ERROR);
    }
    finally
    {
      closeSocketCon(socket);
    }
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
