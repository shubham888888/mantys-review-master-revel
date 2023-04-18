package com.pearrity.mantys.quickBooks;

import com.pearrity.mantys.interfaces.RequestType;
import com.pearrity.mantys.utils.UtilFunctions;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QuickBooksResourceUtil {
  public static final String ProfitAndLossReport = "ProfitAndLoss";
  public static final String ProfitAndLossDetail = "ProfitAndLossDetail";
  public static final String Attachable = "Attachable";
  public static final String Invoice = "Invoice";
  public static final String Customer = "Customer";
  public static final String Attachable2 = "Attachable2";
  public static final String platform = "quickbooks";
  private static final Logger log = Logger.getLogger(QuickBooksResourceUtil.class.getName());

  public static Map<String, Object> executeQuery(
      String query, QuickBooksAuthUtil quickBooksAuthUtil, String realmId, String key)
      throws Exception {
    String url =
        String.format(quickBooksAuthUtil.queryBaseUrl, realmId, QuickBooksAuthUtil.minorVersion);
    log.info("url : " + url);
    log.info("query : " + query);
    RequestBody requestBody = RequestBody.create(MediaType.parse("application/text"), query);
    Request request =
        UtilFunctions.createPostRequest(url, quickBooksAuthUtil.getAccessToken(), requestBody);
    JSONObject jsonObject =
        UtilFunctions.makeWebRequest(
                query,
                request,
                quickBooksAuthUtil.getTenant(),
                quickBooksAuthUtil,
                true,
                RequestType.Loader,
                key,
                platform,
                realmId)
            .getJSONObject("QueryResponse");
    boolean repeat = false;
    try {
      repeat = jsonObject.getJSONArray(key).length() == 30;
      return Map.of("array", jsonObject.getJSONArray(key), "repeat", repeat);
    } catch (Exception e) {
      log.log(Level.SEVERE, e.getMessage());
      return Map.of("array", new JSONArray(), "repeat", repeat);
    }
  }
}
