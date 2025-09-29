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
        executor.submit(() -> handleClient(socket));
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

  private void handleClient(Socket socket)
  {
    try
    {
      socket.setSoTimeout(5000);
      //Reader/writer
      BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

      String line = readLineWithLimit(reader, MAX_LINE_LEN);
      if(line == null)
      {
        sendstatus(writer,StatusCodes.UNKNOWN_CMD);
        return;
      }

      //Parse JSON -> Map
      Map<String, String> req = tryParseJsonMap(line);
      if(req == null){
        sendstatus(writer,StatusCodes.UNKNOWN_CMD);
      }
      //Extract fields + IP
      String cmd = req.get("CMD");
      String name = req.get("NAME");
      String ip = socket.getInetAddress().getHostAddress();

      if(cmd == null || name == null){
        sendstatus(writer, StatusCodes.UNKNOWN_CMD);
      }
      cmd = cmd.toUpperCase(Locale.ROOT);

      //Dispatch to service
      try{
        if("REGISTER".equals(cmd)){
          long ttl = registry.register(name,ip);
          sendTtl(writer,ttl);
        }
        else if ("UPDATE".equals(cmd))
        {
          long ttl = registry.update(name, ip);
          sendTtl(writer, ttl);
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
      throw new RuntimeException(e);
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

  private void writeJsonLine(BufferedWriter writer,  Map<String, String> payload)
      throws IOException
  {
    String json = gson.toJson(payload);
    writer.write(json);
    writer.newLine();
    writer.flush();
  }

  private void closeSocketCon(Closeable c)
  {
    try{
    if(c != null)
    {
      c.close();
    }
    }
    catch (IOException e)
    {
      //ignore
    }
  }
  private String readLineWithLimit(BufferedReader reader,long maxLen){
    try
    {
      String line = reader.readLine();
      if(line == null)
      {
        return null;
      }
      if(line.length() > maxLen)
      {
        return null;
      }
      return line.trim();
    }
    catch (IOException e)
    {
      throw new RuntimeException("Could not read line: "+e);
    }
  }

  private Map<String,String> tryParseJsonMap(String text)
  {
    try
    {
      @SuppressWarnings("unchecked") Map<String, Object> raw = gson.fromJson(
          text, Map.class);
      if (raw == null)
        return null;

      //Konverter værdier til strenge
      Map<String, String> out = new java.util.HashMap<>();
      for (Map.Entry<String, Object> entry : raw.entrySet())
      {
        out.put(entry.getKey(),
            entry.getValue() == null ? null : String.valueOf(entry.getValue()));

      }
      return out;
    } catch (JsonSyntaxException ex)
    {
      return null;
    }
  }
  private void sendTtl(BufferedWriter writer,  long ttlSec) throws IOException
  {
    String ttlStr = format6(ttlSec);
    Map<String,String> map = new HashMap<>();
    map.put("TTL", ttlStr);

    writeJsonLine(writer, map);
  }

  private String format6(long n)
  {
    return String.format("%06d",n);
  }

}
