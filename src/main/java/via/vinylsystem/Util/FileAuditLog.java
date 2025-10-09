package via.vinylsystem.Util;

import com.google.gson.Gson;
import via.vinylsystem.Model.RegistryEvent;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileAuditLog implements AuditLog
{
  private final BufferedWriter out;
  private final Gson gson = new Gson();

  public FileAuditLog(Path path) throws IOException
  {
    if(path.getParent()!=null)
      Files.createDirectories(path.getParent());
      this.out = Files.newBufferedWriter(
          path, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
          StandardOpenOption.APPEND);
  }

  @Override public synchronized void append(RegistryEvent e)
  {
    try{
      out.write(gson.toJson(e));
      out.write("\n");
      out.flush();
    }
    catch (IOException ex)
    {
      System.out.println("Audit write faield: " + ex.getMessage());
    }

  }

  @Override public synchronized void close() throws Exception
  {
    AuditLog.super.close();
  }
}
