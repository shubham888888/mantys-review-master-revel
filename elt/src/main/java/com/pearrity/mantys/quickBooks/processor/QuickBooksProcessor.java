package com.pearrity.mantys.quickBooks.processor;

import com.pearrity.mantys.quickBooks.QuickBooksResourceUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.pearrity.mantys.utils.UtilFunctions.getObjectOrDefault;

public class QuickBooksProcessor implements ItemProcessor<JSONObject, JSONArray> {

  private static final Logger log = Logger.getLogger(QuickBooksProcessor.class.getName());
  private final String type;
  private final Function<JSONObject, JSONArray> processAttachable =
      (a) -> {
        JSONArray array = new JSONArray();
        try {
          a.put("last_modified_time", a.getJSONObject("MetaData").get("LastUpdatedTime"));
          if (!a.has("AttachableRef")) return null;
          a.put(
              "type_id",
              a.getJSONArray("AttachableRef")
                  .getJSONObject(0)
                  .getJSONObject("EntityRef")
                  .getString("value"));
          a.put(
              "type",
              a.getJSONArray("AttachableRef")
                  .getJSONObject(0)
                  .getJSONObject("EntityRef")
                  .getString("type"));
          array.put(a);
        } catch (Exception e) {
          log.log(Level.SEVERE, e.getMessage());
        }
        return array;
      };
  private final Function<JSONObject, JSONArray> processInvoice =
      (a) -> {
        JSONArray array = new JSONArray();
        a.put("last_modified_time", a.getJSONObject("MetaData").get("LastUpdatedTime"));
        a.put("customer_id", a.getJSONObject("CustomerRef").getString("value"));
        a.put("customer_name", a.getJSONObject("CustomerRef").getString("name"));
        array.put(a);
        return array;
      };

  private final Function<JSONObject, JSONArray> processCustomer =
      (a) -> {
        JSONArray array = new JSONArray();
        a.put("last_modified_time", a.getJSONObject("MetaData").get("LastUpdatedTime"));
        array.put(a);
        return array;
      };
  private static final Function<JSONObject, JSONArray> reportProcessor =
      (a) -> {
        JSONObject header;
        try {
          header = a.getJSONObject("Header");
          if (isReportDataEmpty(header)) {
            return null;
          }
        } catch (Exception e) {
          log.log(Level.SEVERE, "");
          return null;
        }
        JSONArray rows = a.getJSONObject("Rows").getJSONArray("Row");
        JSONArray columns = a.getJSONObject("Columns").getJSONArray("Column");
        JSONArray res = new JSONArray();
        List<String> key2s = new ArrayList<>();
        for (Object col : columns) {
          JSONObject jsonObject = (JSONObject) col;
          key2s.add(jsonObject.getString("ColTitle"));
        }
        key2s.add("type_id");
        List<List<Object>> rowValues = new ArrayList<>();
        try {
          getRows("", rowValues, rows);
        } catch (Exception e) {
          log.log(Level.SEVERE, "error :: " + e.getMessage());
        }
        for (List<Object> objects : rowValues) {
          try {
            for (int i = 1; i < key2s.size(); i++) {
              JSONObject object = new JSONObject();
              object.put("full_key1", objects.get(0));
              object.put("uuid", objects.get(objects.size() - 1));
              object.put("key2", key2s.get(i));
              object.put("value", objects.get(i + 1));
              object.put("key1", objects.get(1));
              object.put("customer_id", "N/A");
              object.put("vt_begin", header.getString("StartPeriod"));
              object.put("vt_end", header.getString("EndPeriod"));
              object.put("last_modified_time", Instant.now().toString());
              res.put(object);
            }
          } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage());
          }
        }
        return res;
      };

  private static boolean isReportDataEmpty(JSONObject header) {
    JSONArray array = header.getJSONArray("Option");
    for (Object o : array) {
      JSONObject ob = (JSONObject) o;
      if (ob.getString("Name").equals("NoReportData")) return ob.getBoolean("Value");
    }
    return true;
  }

  private static void getRows(String parent, List<List<Object>> list, JSONArray rows) {
    for (Object ob : rows) {
      JSONObject object = (JSONObject) ob;
      if (object.has("type") && Objects.equals(object.getString("type"), "Section")) {
        {
          if (object.has("Header")) getSection(parent, list, object.getJSONObject("Header"));
        }
        {
          if (object.has("Rows") && object.getJSONObject("Rows").has("Row")) {
            getRows(
                (String) list.get(list.size() - 1).get(0),
                list,
                object.getJSONObject("Rows").getJSONArray("Row"));
          } else if (object.has("Rows")) {
            log.info("");
          } else {
            log.info("");
          }
        }
        {
          if (object.has("Summary")) getSection(parent, list, object.getJSONObject("Summary"));
        }
      } else {
        getSection(parent, list, object);
      }
    }
  }

  private static void getSection(String parent, List<List<Object>> list, JSONObject object) {
    try {
      JSONArray headers = object.getJSONArray("ColData");
      List<Object> headerValueList = getFinalPojoListFromJSONArray(headers, parent);
      list.add(headerValueList);
    } catch (Exception ignored) {
      log.log(Level.SEVERE, "");
    }
  }

  private static List<Object> getFinalPojoListFromJSONArray(JSONArray headers, String parent) {
    List<Object> pojoList = new ArrayList<>();
    StringBuilder typeId = new StringBuilder();
    headers.forEach(
        k -> {
          JSONObject m = (JSONObject) k;
          pojoList.add(m.get("value"));
          if (typeId.isEmpty()) typeId.append(m.has("id") ? m.getString("id") : "");
        });
    String key = (String) pojoList.get(0);
    String firstVal = parent + ":" + key;
    pojoList.add(0, firstVal);
    pojoList.add(typeId.toString());
    pojoList.add(UUID.randomUUID().toString());
    return pojoList;
  }

  public QuickBooksProcessor(String type) {
    this.type = type;
  }

  @Override
  public JSONArray process(@NonNull JSONObject item) {
    if (Objects.equals(type, QuickBooksResourceUtil.ProfitAndLossReport)
        || Objects.equals(type, QuickBooksResourceUtil.ProfitAndLossDetail)) {
      return reportProcessor.apply(item);
    } else if (Objects.equals(type, QuickBooksResourceUtil.Invoice)) {
      Object idVal = getObjectOrDefault(item, "Id", null);
      if (Objects.isNull(idVal)) return null;
      return processInvoice.apply(item);
    } else if (Objects.equals(type, QuickBooksResourceUtil.Customer)) {
      return processCustomer.apply(item);
    } else {
      return processAttachable.apply(item);
    }
  }
}
