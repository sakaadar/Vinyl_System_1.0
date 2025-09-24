package via.vinylsystem.client;

import java.io.IOException;
import java.net.*;

public class EchoClient
{
  private DatagramSocket socket;
  private InetAddress address;

  private byte[] buf;

  public EchoClient() throws SocketException
  {
    try
    {
      socket = new DatagramSocket();
      address = InetAddress.getByName("localhost");
    }
    catch (UnknownHostException e)
    {
      throw new RuntimeException("Uknown host: " + e);
    }
  }

   public String sendEcho(String msg) throws IOException
   {
     buf = msg.getBytes();
     DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 4445);
     socket.send(packet);
     String received = new String(packet.getData(),0, packet.getLength());
     return received;
   }
   public void close(){
    socket.close();
   }
}
