package com.pearrity.mantys.salesforce.processor;

import com.pearrity.mantys.domain.utils.Constants;
import com.pearrity.mantys.interfaces.RequestType;
import com.pearrity.mantys.salesforce.SalesforceAuthUtil;
import com.pearrity.mantys.utils.UtilFunctions;
import com.squareup.okhttp.Request;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.batch.item.ItemProcessor;

import java.util.Base64;

import static com.pearrity.mantys.domain.utils.Constants.SForceLocator;
import static com.pearrity.mantys.domain.utils.Constants.responseHeadersFromResponse;
import static com.pearrity.mantys.salesforce.SalesforceResourceUtil.platform;
import static com.pearrity.mantys.salesforce.SalesforceResourceUtil.updateJobStatusInDB;
import static com.pearrity.mantys.utils.UtilFunctions.addToUnsuccessfulSync;
import static com.pearrity.mantys.utils.UtilFunctions.convertPlainCsvStringToJsonString;

@Slf4j
public class SalesforceProcessor implements ItemProcessor<String, JSONArray> {

  private final String type;

  private final SalesforceAuthUtil salesforceAuthUtil;

  public SalesforceProcessor(String type, SalesforceAuthUtil authUtil) {
    this.type = type;
    salesforceAuthUtil = authUtil;
  }

  public JSONArray processAdapter(String id) {
    try {
      return processMain(id, type, salesforceAuthUtil);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      addToUnsuccessfulSync(id, type, salesforceAuthUtil.getTenant(), platform, null);
      return null;
    }
  }

  private JSONArray processMain(String id, String type, SalesforceAuthUtil salesforceAuthUtil)
      throws Exception {
    final String url = salesforceAuthUtil.getBulkJobQueryUrl() + "/" + id + "/results";
    final String param = "?locator=%s&maxRecords=1000";
    String locator = null;
    boolean controlVar = true;
    JSONArray arr = new JSONArray();
    do {
      String finalUrl;
      if (locator != null) finalUrl = url + param.trim().formatted(locator);
      else finalUrl = url;
      Request request =
          UtilFunctions.createGetRequestWithAcceptHeader(
              finalUrl, salesforceAuthUtil.getAccessToken(), Constants.TEXT_CSV);
      JSONObject ob =
          UtilFunctions.makeWebRequest(
              null,
              request,
              salesforceAuthUtil.getTenant(),
              salesforceAuthUtil,
              false,
              RequestType.Reader,
              type,
              platform,
              null);
      try {
        JSONObject headers;
        if (ob.has(responseHeadersFromResponse)) {
          headers = ob.getJSONObject(responseHeadersFromResponse);
          if (headers.has(SForceLocator)) {
            locator = headers.getJSONArray(SForceLocator).getString(0);
            if (locator.equalsIgnoreCase(Constants.nullValue)) {
              locator = null;
              controlVar = false;
            }
          } else {
            controlVar = false;
          }
        }
      } catch (Exception e) {
        locator = null;
        controlVar = false;
      }
      convertPlainCsvStringToJsonString(
              new String(Base64.getDecoder().decode(ob.getString("data"))))
          .forEach(arr::put);
    } while (controlVar);
    closeSalesForceJob(id, salesforceAuthUtil);
    updateJobStatusInDB(salesforceAuthUtil, id, Constants.success);
    return arr;
  }

  private void closeSalesForceJob(String id, SalesforceAuthUtil salesforceAuthUtil)
      throws Exception {
    String finalUrl = salesforceAuthUtil.getBulkJobQueryUrl() + "/" + id;
    Request request =
        UtilFunctions.createDeleteRequest(finalUrl, salesforceAuthUtil.getAccessToken());
    UtilFunctions.makeWebRequest(
        null,
        request,
        salesforceAuthUtil.getTenant(),
        salesforceAuthUtil,
        true,
        RequestType.Reader,
        type,
        platform,
        null);
  }

  @Override
  public JSONArray process(@NonNull String id) {
    return processAdapter(id);
  }
}
