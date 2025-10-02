package via.vinylsystem.directory;

import java.time.Clock;

public class DirectoryMain
{
  public static void main(String[] args)
  {
    int tcpPort = 5044;
    int udpPort = 4555;

    long defaultTtlSec = 3600;

    RegistryService registry = new RegistryService(defaultTtlSec, Clock.systemUTC());

    DirectoryTCPServer tcpServer = new DirectoryTCPServer(tcpPort,registry);
    DirectoryUDPServer udpServer = new DirectoryUDPServer(udpPort, registry);

    try{
      tcpServer.start();
      udpServer.start();
      System.err.printf("Directory running. TCP:%d, UDP:%d, TTL:%ds%n",tcpPort,udpPort,defaultTtlSec);
    } catch (Exception e)
    {
      System.err.println("Failed to start Directory: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Shutting down Directory...");
      try{tcpServer.stop();} catch(Exception ignored) {};
      try{udpServer.stop();} catch(Exception ignored) {};
      System.out.println("Directory stopped.");
    }));

  }
}
