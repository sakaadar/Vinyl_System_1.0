package via.vinylsystem.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import via.vinylsystem.Model.Track;
import via.vinylsystem.Util.JsonUtils;
import via.vinylsystem.directory.RegistryService;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Servermain
{
  public static void main(String[] args) throws IOException
  {
    String serverName = getArg(args,0,System.getenv().getOrDefault("VINNYL_NAME", "Happy_music.group3.pro2"));
    int servicePort = Integer.parseInt(System.getenv().getOrDefault("VINYL_PORT", "7070"));
    String directoryHost = System.getenv().getOrDefault("VINYL_DIR_HOST","127.0.0.1");
    int directoryTcp = Integer.parseInt(System.getenv().getOrDefault("VINYL_DIR_TCP","5044"));
    int ttlSec = Integer.parseInt(System.getenv().getOrDefault("VINYL_TTL", "900"));

    if(!RegistryService.validName(serverName)) throw new IllegalArgumentException("Ugyldigt navn: " + serverName);

    //Start TCP-Forbindelse til directory
    List<Track> catalog = CatalogServer.seedCatalog();
    CatalogServer srv = new CatalogServer(servicePort, catalog);
    srv.start();

    //Find ip
    String ip = System.getenv().getOrDefault("VINYL_IP", "127.0.0.1");

    //Register to directory
    sendToDirectory("REGISTER",serverName,ip,ttlSec,directoryHost,directoryTcp);

    //RENEW Connection (TTL/2)
    ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
    ses.scheduleAtFixedRate(() ->{
      try{sendToDirectory("RENEW",serverName,ip,ttlSec,directoryHost,directoryTcp);
      }

      catch (Exception e) {System.err.println("RENEW Failed: " + e.getMessage());}
    }, ttlSec / 2, ttlSec / 2, TimeUnit.SECONDS);



  }
  private static void sendToDirectory(String cmd,String name, String ip, int ttlSec,
                                      String host, int port){
    JsonObject msg = new JsonObject();
    msg.addProperty("CMD", cmd);
    msg.addProperty("NAME", name);
    msg.addProperty("IPv4", ip);
    msg.addProperty("TTL", JsonUtils.ttl6(ttlSec));

    try(Socket s = new Socket())
    {
      s.connect(new InetSocketAddress(host,port),2000);
      BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(),
          StandardCharsets.UTF_8));
      BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));

      out.write(msg.toString());
      out.write("\n");
      out.flush();

      String line = in.readLine();
      if(line == null) throw new EOFException("Directory lukkede forbindelsen");
      JsonObject resp = JsonParser.parseString(line).getAsJsonObject();
      String status = resp.has("STATUS") ? resp.get("STATUS").getAsString() : "??????";
      if(!"000000".equals(status))
      {
        throw new IOException("Directory STATUS=" + status + " for " + cmd);
      }
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  private static String getArg(String[] args, int idx, String def){
    return (args != null && args.length > idx && args[idx] != null && !args[idx].isBlank()) ? args[idx] : def;
  }
}
