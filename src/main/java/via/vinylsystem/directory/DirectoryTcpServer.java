package via.vinylsystem.directory;

import com.cedarsoftware.io.JsonWriter;
import com.google.gson.Gson;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DirectoryTcpServer
{
  private int port;
  private static final Gson gson = new Gson(); //Konvertere til JSON
  private RegistryService registry;
  private ServerSocket serverSocket;
  private ExecutorService executor;
  private boolean running;

  public DirectoryTcpServer(int port, RegistryService registry)
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

      String line = reader.readLine();
      if(line == null)
      {
       // sendstatus(writer,);
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


}
