package com.pearrity.mantys.chargebee.processor;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;

import java.util.logging.Logger;

import static com.pearrity.mantys.chargebee.ChargeBeeResourceUtil.*;

public class ChargeBeeProcessor implements ItemProcessor<JSONObject, JSONArray> {

  private static final Logger log = Logger.getLogger(ChargeBeeProcessor.class.getName());
  private final String type;

  public ChargeBeeProcessor(String type) {
    this.type = type;
  }

  @Override
  public JSONArray process(@NonNull JSONObject item) {
    JSONArray array = new JSONArray();
    if (type.equalsIgnoreCase(Invoices) || type.equalsIgnoreCase(CreditNotes)) {
      processLineItems(item);
    }
    return array.put(item);
  }

  private void processLineItems(JSONObject item) {
    if (item.has(LineItem)) {
      JSONArray newLineItems = new JSONArray();
      JSONArray arr = item.getJSONArray(LineItem);
      for (Object ob : arr) {
        JSONObject jsonObject = (JSONObject) ob;
        jsonObject.put(updatedAt, item.get(updatedAt));
        jsonObject.put("parent_id", item.getString("id"));
        newLineItems.put(jsonObject);
      }
      item.put(LineItem, newLineItems);
    }
  }
}
