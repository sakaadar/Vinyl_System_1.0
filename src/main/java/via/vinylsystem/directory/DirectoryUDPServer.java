package via.vinylsystem.directory;

import java.io.Closeable;

import via.vinylsystem.Model.Registration;
import via.vinylsystem.Util.JsonUtils;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static via.vinylsystem.Util.JsonUtils.format6;
import static via.vinylsystem.Util.JsonUtils.tryParseJsonMap;

public class DirectoryUDPServer
{
  private int port;
  private RegistryService registry;
  private DatagramSocket socket;
  private volatile boolean running;

  private static final int MAX_UDP = 2048;

  public DirectoryUDPServer(int port, RegistryService registry)
  {
    this.port = port;
    this.registry = registry;
  }
  public void start(){
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
    try{
      //læs antal bytes -> omdan til string -> også trim
      String text = new String(
          packet.getData(),
          packet.getOffset(),
          packet.getLength(),
          StandardCharsets.UTF_8
      ).trim();
      //Parse to json
      Map<String, String> req = tryParseJsonMap(text);
        if(req == null)
        {
          sendJson(packet.getAddress(),packet.getPort(),Map.of("STATUS: ",StatusCodes.UNKNOWN_CMD));
        }
      //Tjek name og ip
      boolean hasName = req.containsKey("NAME");
      boolean hasIp = req.containsKey("IP");

      if(!hasName && !hasIp)
      {
        sendJson(packet.getAddress(), packet.getPort(), Map.of("STATUS: ",StatusCodes.UNKNOWN_CMD));
      }
      //slå op i registry
      try{
        Registration reg;
        if(hasName){
          String name = req.get("NAME");
          if(name==null || name.isEmpty()){
            sendJson(packet.getAddress(), packet.getPort(), Map.of("STATUS: ", StatusCodes.UNKNOWN_CMD));
          }
          reg = registry.findByName(name);
        }
        else{
          String ipReq = req.get(("IP"));
          if(ipReq==null || ipReq.isEmpty())
          {
            sendJson(packet.getAddress(), packet.getPort(), Map.of("STATUS: ", StatusCodes.UNKNOWN_CMD));
          }
          reg = registry.findByIp(ipReq);
        }
        //Hvis fundet beregn ttl og send svar
        long nowMs = System.currentTimeMillis();
        long ttlLeftSec = Math.max(0L,(reg.getExpiresAtMillis()- nowMs / 1000L));

        Map<String,String> response = new HashMap<>();
        response.put("NAME", reg.getName());
        response.put("IPv4", reg.getIp());
        response.put("TTL", format6(ttlLeftSec));
        sendJson(packet.getAddress(), packet.getPort(),response);
      }
      catch (StatusExeption e)
      {
        sendJson(packet.getAddress(), packet.getPort(),Map.of("STATUS: ", e.getCode()));
      }
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }
  private void sendJson(InetAddress address, int port, Map<String,String> payload)
  {
    try
    {
    String json = JsonUtils.toJson(payload);

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
