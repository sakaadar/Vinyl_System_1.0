package via.vinylsystem.server;
import via.vinylsystem.Model.Track;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static via.vinylsystem.Util.JsonUtils.*;

public class CatalogServer
{
  private final int servicePort;
  private ServerSocket server;
  private ExecutorService clientPool;
  private volatile boolean running;
  private List<via.vinylsystem.Model.Track> catalog;

  public CatalogServer(int servicePort, List<via.vinylsystem.Model.Track> catalog)
  {
    this.servicePort = servicePort;
    this.catalog = catalog;
    this.running = false;
  }

  public void start() throws IOException
  {
    server = new ServerSocket(servicePort);
    clientPool = Executors.newCachedThreadPool();
    running = true;

    Thread t = new Thread(this::acceptLoop, "catalog-accept");
    t.start();
  }

  private void acceptLoop()
  {
    while(running)
    {
      try
      {
        Socket s = server.accept();
        clientPool.submit(()->handleClient(s));
      }
      catch (IOException e)
      {
        throw new RuntimeException("Could not accept connections: "+e);
      }
    }
  }

  private void handleClient(Socket s)
  {
    //læs en linje (LIST / GET / SEARCH)
    //byg svar som JSON-linje
    //skriv + flush, luk socket
    try
    {
      s.setSoTimeout(5000);
      BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));

      String line = readLineWithLimit(reader, 2048);
      if(line == null)
      {
        writeJsonLine(writer, Map.of("STATUS","BAD_REQ"));
        closeSocketCon(s);
      }
      else if(line.equals("LIST"))
      {
        Map<String,String> payload = new HashMap<>();

        for(Track t : catalog)
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
  private static List<Track> seedCatalog(){
    return List.of(
        new Track("T001","Gilli", "La Varrio", 2017),
        new Track("T002", "Kesi", "Mamacita", 2014),
        new Track("T003", "Medina", "Kun for mig", 2009),
        new Track("T004", "L.O.C.", "Frk. Escobar", 2004),
        new Track("T005", "Nik & Jay", "Boing!", 2006),
        new Track("T006", "Malk de Koijn", "Vi tager fuglen på dig", 2002),
        new Track("T007", "Suspekt", "Søndagsbarn", 2014),
        new Track("T008", "Barbara Moleko", "Dum for dig", 2012),
        new Track("T009", "Christopher", "Told You So", 2014),
        new Track("T010", "Rasmus Seebach", "Lidt i fem", 2009)
    );
  }
}
