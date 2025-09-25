package via.vinylsystem.client;


public class LookupDemo {
  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.out.println("Usage: LookupDemo <directoryHost> <name>");
      return;
    }
    DirectoryLookupClient c = new DirectoryLookupClient(args[0], 4445);
    System.out.println("LOOKUP response: " + c.lookup(args[1]));
  }
}
