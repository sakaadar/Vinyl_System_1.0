package via.vinylsystem.directory;

import java.io.Closeable;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static via.vinylsystem.directory.DirectoryTCPServer.gson;

public class DirectoryUDPServer
{
  private int port;
  private RegistryService registry;
  private DatagramSocket socket;
  private volatile boolean running;

  private static final int MAX_UDP = 2048;
  private static final Gson GSON = new Gson();

  public DirectoryUDPServer(int port, RegistryService registry)
  {
    this.port = port;
    this.registry = registry;
  }
  private void start(){
    try
    {
      socket = new DatagramSocket(port);
      running = true;
      new Thread(this::receiveLoop).start();

    }
    catch (SocketException e)
    {
      throw new RuntimeException("Could not connect: "+e);
    }
  }
  public void stop(){
    running = false;
    closeSocket(socket);
  }

  private void receiveLoop()
  {
    byte[] buffer = new byte[MAX_UDP];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

    while(running)
    {
      try
      {
        socket.receive(packet);
        handlePacket(packet);

        //reset packet length
        packet.setLength(buffer.length);
      }
      catch(SocketException e){
        break;
      }
      catch (IOException e)
      {
        System.out.println("UDP receive error"+e);
      }
    }
  }

  private void handlePacket(DatagramPacket packet)
  {


  }
  private void sendJson(InetAddress address, int port, Map<String,String> payload)
  {
    try
    {
    String json = GSON.toJson(payload);

    //transform JSON -> bytes
    byte[] data = json.getBytes(StandardCharsets.UTF_8);

    //Lav UDP-pakke
    DatagramPacket outPacket = new DatagramPacket(data, data.length, address, port);

    //Send via socket
      socket.send(outPacket);
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  private void closeSocket(Closeable c)
  {
    try{
      if(c != null)
      {
        c.close();
      }
    }
    catch (IOException e)
    {
      //Ignore
    }
  }
}
