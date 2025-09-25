package via.vinylsystem.server;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class EchoServer extends Thread
{
  private DatagramSocket socket;
  private boolean running;
  private byte[] buf = new byte[256];
  private Map<String, Vinyl> vinyls;

  public EchoServer() throws SocketException
  {
    socket = new DatagramSocket(4445);
    loadMockData();
  }

  private void loadMockData() {
    vinyls = new HashMap<>();
    vinyls.put("Bohemian Rhapsody", new Vinyl("Queen", "Bohemian Rhapsody", 1975));
    vinyls.put("Like a Rolling Stone", new Vinyl("Bob Dylan", "Like a Rolling Stone", 1965));
    vinyls.put("Stairway to Heaven", new Vinyl("Led Zeppelin", "Stairway to Heaven", 1971));
    vinyls.put("Smells Like Teen Spirit", new Vinyl("Nirvana", "Smells Like Teen Spirit", 1991));
  }

  public void run(){
    running = true;

    while(running){
      try
      {
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);

        InetAddress address = packet.getAddress();
        int port = packet.getPort();

        String received = new String(packet.getData(),0, packet.getLength()).trim();

        if(received.equals("end")){
          running = false;
          continue;
        }

        Vinyl vinyl = vinyls.get(received);
        String response;
        if (vinyl != null) {
          response = vinyl.toJson();
        } else {
          response = "{\"error\":\"Vinyl not found\"}";
        }

        byte[] responseBuf = response.getBytes();
        packet = new DatagramPacket(responseBuf, responseBuf.length, address, port);
        socket.send(packet);

      }
      catch (IOException e)
      {
        // Consider logging the error instead of throwing a RuntimeException in a thread
        e.printStackTrace();
        running = false;
      }
    }
    socket.close();
  }
}