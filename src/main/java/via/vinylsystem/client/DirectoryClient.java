package via.vinylsystem.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class DirectoryClient
{
  private final InetAddress dirAddr;
  private final int dirPort;

  public static record Resolve(String name, String ip, int ttlSec){}

  public DirectoryClient(String directoryHost, int directoryUdpPort)
      throws UnknownHostException
  {
    this.dirAddr = InetAddress.getByName(directoryHost);
    this.dirPort = directoryUdpPort;
  }

  //slår Server-IP op via DirectoryUDPServer med UDP
  public Resolve resolveByName(String name){
    try(DatagramSocket socket = new DatagramSocket()){
      socket.setSoTimeout(1500);
      //Send request to server
      String reqJson = "{\"NAME\":\"" + name + "\"}";
      byte[] data = reqJson.getBytes(StandardCharsets.UTF_8);
      socket.send(new DatagramPacket(data,data.length,dirAddr,dirPort));

      //modtag svar
      byte[] buf = new byte[2048];
      DatagramPacket resp = new DatagramPacket(buf, buf.length);
      socket.receive(resp);

      String json = new String(resp.getData(), resp.getOffset(), resp.getLength(), StandardCharsets.UTF_8);
      JsonObject object = JsonParser.parseString(json).getAsJsonObject();

      //parse svar
      if(object.has("IPv4")){
        String ip = object.get("IPv4").getAsString();
        int ttlSec = Integer.parseInt(object.get("TTL").getAsString()); //"000900" -> 900
        String outName = object.get("NAME").getAsString();
        return new Resolve(outName,ip,ttlSec);
      }else{
        String status = object.has("STATUS") ? object.get("STATUS").getAsString() : "??????";
        throw new IOException("Directory error STATUS=" + status);
      }
    }
    catch (SocketException e)
    {
      throw new RuntimeException(e);
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }
}
