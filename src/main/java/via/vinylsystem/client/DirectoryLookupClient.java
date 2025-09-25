package via.vinylsystem.client;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DirectoryLookupClient {
  private final InetAddress address;
  private final int port;

  public DirectoryLookupClient(String host, int port) throws Exception {
    this.address = InetAddress.getByName(host);
    this.port = port;
  }

  public String lookup(String name) throws Exception {
    try (DatagramSocket socket = new DatagramSocket()) {
      byte[] out = ("LOOKUP " + name).getBytes();
      socket.send(new DatagramPacket(out, out.length, address, port));
      byte[] buf = new byte[512];
      DatagramPacket resp = new DatagramPacket(buf, buf.length);
      socket.receive(resp);
      return new String(resp.getData(), 0, resp.getLength());
    }
  }
}