package via.vinylsystem.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class VinylServerClient implements Closeable
{
  private final Socket sock;
  private final BufferedReader in;
  private final BufferedWriter out;

  public VinylServerClient(String ip, int port) throws IOException
  {
    this.sock = new Socket();
    this.sock.connect(new InetSocketAddress(ip,port),2000);
    this.in = new BufferedReader(new InputStreamReader(sock.getInputStream(),
        StandardCharsets.UTF_8));
    this.out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8));
  }
  private JsonObject send(JsonObject msg) throws IOException
  {
    out.write(msg.toString());
    out.write("\n");
    out.flush();
    String line = in.readLine();
    if(line==null) throw new EOFException("Server closed");
    return JsonParser.parseString(line).getAsJsonObject();
  }

  public JsonObject list() throws IOException
  {
    JsonObject q = new JsonObject();
    q.addProperty("CMD","LIST");
    return send((q));
  }
  public JsonObject search(String qstr) throws IOException
  {
    JsonObject q = new JsonObject();
    q.addProperty("CMD","SEARCH");
    q.addProperty("Q", qstr);
    return send(q);
  }
  public String get(String id) throws IOException
  {
      JsonObject q = new JsonObject();
      q.addProperty("CMD","GET");
      q.addProperty("ID", id);
      return send(q).toString();
  }

  @Override public void close() throws IOException
  {
    sock.close();
  }
}
