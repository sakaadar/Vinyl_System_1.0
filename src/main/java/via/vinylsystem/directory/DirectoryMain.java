package via.vinylsystem.directory;

import java.time.Clock;

public class DirectoryMain {
  public static void main(String[] args) throws Exception {
    long defaultTtlSeconds = 60; // adjust as needed
    RegistryService registry = new RegistryService(defaultTtlSeconds, Clock.systemUTC());

    int tcpPort = 5000;
    int udpPort = 4445;

    DirectoryTcpServer tcp = new DirectoryTcpServer(tcpPort, registry);
    DirectoryUdpServer udp = new DirectoryUdpServer(udpPort, registry);
    tcp.start();
    udp.start();

    System.out.println("Directory started. TCP=" + tcpPort + " UDP=" + udpPort);
    System.out.println("Press Ctrl+C to stop.");
    // Keep main thread alive
    while (true) {
      Thread.sleep(60_000);
    }
  }
}