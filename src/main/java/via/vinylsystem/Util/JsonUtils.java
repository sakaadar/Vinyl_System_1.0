package via.vinylsystem.Util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.Map;

public class JsonUtils
{
  private static final Gson gson = new Gson();

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

}
