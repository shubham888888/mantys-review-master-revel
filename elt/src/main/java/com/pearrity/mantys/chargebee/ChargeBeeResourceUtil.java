package com.pearrity.mantys.chargebee;

import com.pearrity.mantys.interfaces.RequestType;
import com.squareup.okhttp.Request;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.pearrity.mantys.utils.UtilFunctions.*;

public class ChargeBeeResourceUtil {
  public static final String Invoices = "invoices";
  public static final String Customers = "customers";
  public static final String CreditNotes = "credit_notes";
  public static final Map<String, String> endPointLookupMap =
      Map.of(
          Invoices,
          "/api/v2/invoices",
          Customers,
          "/api/v2/customers",
          CreditNotes,
          "/api/v2/credit_notes");
  public static final String platform = "chargebee";
  private static final Logger log = Logger.getLogger(ChargeBeeResourceUtil.class.getName());
  public static String LineItem = "line_items";
  public static String updatedAt = "updated_at";

  public static Map<String, Object> executeEntityListRequest(
      ChargeBeeAuthUtil chargeBeeAuthUtil, String type, String url, String param) throws Exception {
    url += ("?" + param);
    log.info(url);
    Request request = createGetRequest(url, chargeBeeAuthUtil.getAccessToken());
    type = type.substring(0, type.length() - 1);
    JSONObject jsonObject =
        makeWebRequest(
            null,
            request,
            chargeBeeAuthUtil.getTenant(),
            chargeBeeAuthUtil,
            true,
            RequestType.Loader,
            type,
            platform,
            null);
    JSONArray array = new JSONArray();
    if (!jsonObject.has("list")) {
      log.log(Level.SEVERE, "list not found for " + type);
      return Map.of("array", array, "next", Strings.EMPTY);
    }
    for (Object item : jsonObject.getJSONArray("list")) {
      JSONObject itemJson = (JSONObject) item;
      array.put(itemJson.getJSONObject(type));
    }
    return Map.of(
        "array",
        array,
        "next",
        jsonObject.has("next_offset") ? jsonObject.getString("next_offset") : Strings.EMPTY);
  }

  public static JSONObject executeSingleEntityRequest(
      ChargeBeeAuthUtil chargeBeeAuthUtil, String type, String url) throws Exception {
    log.info(url);
    Request request = createGetRequest(url, chargeBeeAuthUtil.getAccessToken());
    type = type.substring(0, type.length() - 1);
    JSONObject jsonObject =
        makeWebRequest(
            null,
            request,
            chargeBeeAuthUtil.getTenant(),
            chargeBeeAuthUtil,
            true,
            RequestType.Reader,
            type,
            platform,
            null);
    return jsonObject.getJSONObject(type);
  }
}
