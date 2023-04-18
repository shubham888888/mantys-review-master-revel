package com.pearrity.mantys.hubspot.processor;

import com.pearrity.mantys.hubspot.HubSpotResourceUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;

import java.util.logging.Level;
import java.util.logging.Logger;

public class HubSpotProcessor implements ItemProcessor<JSONObject, JSONArray> {

  private static final Logger log = Logger.getLogger(HubSpotProcessor.class.getName());
  private final String type;

  public HubSpotProcessor(String type) {
    this.type = type;
  }

  public static JSONArray processMain(JSONObject item) {
    try {
      JSONObject item1 = new JSONObject();
      item.keySet()
          .forEach(
              k -> {
                if (!(k.equalsIgnoreCase("properties"))) {
                  item1.put(k, item.get(k));
                }
              });
      JSONObject jsonObject = item.getJSONObject("properties");
      jsonObject.keySet().forEach(a -> item1.put(a, jsonObject.get(a)));
      return new JSONArray().put(item1);
    } catch (Exception e) {
      log.log(Level.SEVERE, e.getMessage());
      if (e.getCause() != null) log.log(Level.SEVERE, e.getCause().getMessage());
    }
    return null;
  }

  @Override
  public JSONArray process(@NonNull JSONObject item) {
    return type.equalsIgnoreCase(HubSpotResourceUtil.Owners)
        ? new JSONArray().put(item)
        : processMain(item);
  }
}
