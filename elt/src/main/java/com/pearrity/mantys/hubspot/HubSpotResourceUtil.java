package com.pearrity.mantys.hubspot;

import com.pearrity.mantys.interfaces.RequestType;
import com.pearrity.mantys.utils.UtilFunctions;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HubSpotResourceUtil {
  public static final String Companies = "Companies";
  public static final String Deals = "Deals";
  public static final String Owners = "Owners";
  public static final String updatedAt = "updatedAt";
  public static final String createdAt = "createdAt";
  public static final Map<String, String> endPointLookupMap =
      Map.of(
          Owners,
          "/crm/v3/owners",
          Deals,
          "/crm/v3/objects/deals",
          Companies,
          "/crm/v3/objects/companies");
  public static final Map<String, String> searchEndPointLookupMap =
      Map.of(Deals, "/crm/v3/objects/deals/search", Companies, "/crm/v3/objects/companies/search");

  public static final String platform = "hubspot";
  private static final Logger log = Logger.getLogger(HubSpotResourceUtil.class.getName());
  private static final String searchBody =
      """
          {
               "sorts": [
                   {
                       "propertyName": "hs_lastmodifieddate",
                       "direction": "ASCENDING"
                   }
               ],
               "filterGroups": [
                   {
                       "filters": [
                           {
                               "propertyName": "hs_lastmodifieddate",
                               "operator": "GTE",
                               "value": "%s"
                           }
                       ]
                   }
               ],
               "limit" : 75,
               "after" : %s
           }
          """;

  public static Map<String, Object> executeEntityRequest(
      HubSpotAuthUtil hubSpotAuthUtil, String type, String params, boolean isSingleEntityRequest)
      throws Exception {
    String url = hubSpotAuthUtil.resourceBaseUrl(type) + params;
    if (isSingleEntityRequest) url = hubSpotAuthUtil.resourceBaseUrl(type) + "/" + params;
    log.info("url : " + url);
    Request request = UtilFunctions.createGetRequest(url, hubSpotAuthUtil.getAccessToken());
    JSONObject jsonObject =
        UtilFunctions.makeWebRequest(
            null,
            request,
            hubSpotAuthUtil.getTenant(),
            hubSpotAuthUtil,
            true,
            RequestType.Reader,
            type,
            platform,
            null);
    if (isSingleEntityRequest) return jsonObject.toMap();
    return processListResponse(jsonObject);
  }

  private static Map<String, Object> processListResponse(JSONObject jsonObject) {
    JSONArray jsonArray = null;
    String after;
    try {
      jsonArray = jsonObject.getJSONArray("results");
      after = jsonObject.getJSONObject("paging").getJSONObject("next").getString("after");
    } catch (Exception e) {
      log.log(
          Level.SEVERE, "if it is paging not found this is an expected error " + e.getMessage());
      after = Strings.EMPTY;
    }
    return Map.of("array", jsonArray == null ? new JSONArray() : jsonArray, "repeat", after);
  }

  public static Map<String, Object> executeSearchRequest(
      HubSpotAuthUtil hubSpotAuthUtil, String type, String params, String lastTime)
      throws Exception {
    {
      String url = hubSpotAuthUtil.resourceSearchUrl(type);
      log.info("url : " + url);
      MediaType mediaType = MediaType.parse("application/json");
      String searchBody = HubSpotResourceUtil.searchBody.formatted(lastTime, params);
      log.info("body : \n" + searchBody);
      RequestBody requestBody = RequestBody.create(mediaType, searchBody);
      Request request =
          UtilFunctions.createPostRequest(url, hubSpotAuthUtil.getAccessToken(), requestBody);
      JSONObject jsonObject =
          UtilFunctions.makeWebRequest(
              searchBody,
              request,
              hubSpotAuthUtil.getTenant(),
              hubSpotAuthUtil,
              true,
              RequestType.Loader,
              type,
              platform,
              null);
      return processListResponse(jsonObject);
    }
  }
}
