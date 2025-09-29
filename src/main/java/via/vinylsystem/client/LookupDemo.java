package via.vinylsystem.client;

public class LookupDemo {
  public static void main(String[] args) throws Exception {
    String directoryHost = args.length > 0 ? args[0] : "localhost";
    String vinylServerName = args.length > 1 ? args[1] : "vinyl.group1.pro2x";

    System.out.println("Looking up server: " + vinylServerName + " at directory: " + directoryHost);

    DirectoryLookupClient c = new DirectoryLookupClient(directoryHost, 4445);
    System.out.println("LOOKUP response: " + c.lookup(vinylServerName));
  }
}