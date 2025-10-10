package via.vinylsystem.directory;

import via.vinylsystem.Util.AuditLog;
import via.vinylsystem.Util.FileAuditLog;
import via.vinylsystem.Util.yamlLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Map;

/**
 * Main entry point for the Directory Service application.
 * <p>
 * This class initializes and starts both TCP and UDP servers for the directory service,
 * which handles service registration, updates, and lookups. The TCP server processes
 * registration and renewal requests, while the UDP server handles lookup queries.
 * </p>
 * <p>
 * Default configuration:
 * <ul>
 *   <li>TCP Port: 5044 (for registration and updates)</li>
 *   <li>UDP Port: 4555 (for lookups)</li>
 *   <li>Default TTL: 3600 seconds (1 hour)</li>
 * </ul>
 * </p>
 * <p>
 * The application registers a shutdown hook to ensure graceful termination of both
 * servers when the JVM exits.
 * </p>
 *
 * @author Ghiyath & sakariae
 * @version 1.0
 */
public class DirectoryMain
{
    /**
     * Starts the Directory Service with TCP and UDP servers.
     * <p>
     * Initializes the registry service with a default TTL of 3600 seconds and starts
     * both TCP and UDP servers on their respective ports. Registers a shutdown hook
     * to ensure proper cleanup when the application terminates.
     * </p>
     * <p>
     * The TCP server listens on port 5044 for registration and update commands,
     * while the UDP server listens on port 4555 for lookup queries.
     * </p>
     *
     * @param args command line arguments (currently unused)
     */
    public static void main(String[] args) throws IOException
    {
        Map<String, Object> config;
        try {
            config = yamlLoader.loadConfig("server_reg_contract.yaml");
        } catch (Exception e) {
            throw new IOException("Failed to load YAML config: " + e.getMessage(), e);
        }
        Map<String, Object> serverConfig = (Map<String, Object>) config.get("server");
        int tcpPort = ((Number) serverConfig.getOrDefault("dir_tcp_port",5044)).intValue();
        int udpPort = ((Number) serverConfig.getOrDefault("dir_udp_port",4555)).intValue();
        long defaultTtlSec = ((Number) serverConfig.getOrDefault("ttl",3600)).longValue();

        Path auditPath = Path.of("./directory-audit.jsonl");
        AuditLog audit = new FileAuditLog(auditPath);

        RegistryService registry = new RegistryService(defaultTtlSec, java.time.Clock.systemUTC(),audit);

        DirectoryTCPServer tcpServer = new DirectoryTCPServer(tcpPort,registry);
        DirectoryUDPServer udpServer = new DirectoryUDPServer(udpPort, registry, audit);

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