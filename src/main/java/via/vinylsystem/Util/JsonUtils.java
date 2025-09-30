package via.vinylsystem.Util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

public class JsonUtils
{
  public static final Gson gson = new Gson();

  public static Map<String, String> tryParseJsonMap(String text){
    try
    {
      @SuppressWarnings("unchecked") Map<String, Object> raw = gson.fromJson(
          text, Map.class);
      if (raw == null)
        return null;

      //Konverter værdier til strenge
      Map<String, String> out = new java.util.HashMap<>();
      for (Map.Entry<String, Object> entry : raw.entrySet())
      {
        out.put(entry.getKey(),
            entry.getValue() == null ? null : String.valueOf(entry.getValue()));

      }
      return out;
    } catch (JsonSyntaxException ex)
    {
      return null;
    }
  }
  public static String toJson(Map<String,String> map){
    return gson.toJson(map);
  }
  public static String format6(long n)
  {
    return String.format("%06d",n);
  }
  public static String readLineWithLimit(BufferedReader reader,long maxLen){
    try
    {
      String line = reader.readLine();
      if(line == null)
      {
        return null;
      }
      if(line.length() > maxLen)
      {
        return null;
      }
      return line.trim();
    }
    catch (IOException e)
    {
      throw new RuntimeException("Could not read line: "+e);
    }
  }

  public static void writeJsonLine(BufferedWriter writer,  Map<String, ?> payload)
      throws IOException
  {
    String json = gson.toJson(payload);
    writer.write(json);
    writer.newLine();
    writer.flush();
  }
  public static void closeSocketCon(Closeable c)
  {
    try{
      if(c != null)
      {
        c.close();
      }
    }
    catch (IOException e)
    {
      //ignore
    }
  }

}
