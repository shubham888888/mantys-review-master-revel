package com.pearrity.mantys.zohoV2.processor;

import com.pearrity.mantys.zoho.ZohoInvoiceAuthUtil;
import com.pearrity.mantys.zoho.ZohoInvoiceResourceUtil;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.batch.item.ItemProcessor;

import java.io.IOException;
import java.net.http.HttpResponse;

import static com.pearrity.mantys.utils.UtilFunctions.makeWebRequest;
import static com.pearrity.mantys.zoho.ZohoInvoiceResourceUtil.*;

@Slf4j
public class ZohoProcessor implements ItemProcessor<String, JSONArray> {

  private final String type;
  private final String organizationId;

  private final ZohoInvoiceAuthUtil authUtil;

  public ZohoProcessor(ZohoInvoiceAuthUtil authUtil, String type, String organizationId)
      throws IOException, InterruptedException {
    this.type = type;
    this.authUtil = authUtil;
    this.organizationId = organizationId;
  }

  public JSONArray processAdapter(String id) throws Exception {
    return processMain(authUtil, id, type, organizationId);
  }

  private JSONArray processMain(
      ZohoInvoiceAuthUtil authUtil, String id, String type, String organizationId)
      throws Exception {
    String endpoint = "/api/v3/" + ZohoInvoiceResourceUtil.stringEndpointLookupMap.get(type);
    String baseUrl = String.format("%s%s", authUtil.resourceBaseUrl, endpoint);
    String individualItemKey = stringEndpointLookupMap.get(type);
    individualItemKey = individualItemKey.substring(0, individualItemKey.length() - 1);
    String urlResource = String.format(baseUrl + "/" + id);
    HttpResponse<String> responseResource =
        (HttpResponse<String>)
            makeWebRequest(
                createRequest(urlResource, authUtil.getAccessToken(), organizationId), null, 0);
    if (checkResponseStatusAndAccessTokenReset(authUtil, responseResource)) {
      return processMain(authUtil, id, type, organizationId);
    }
    JSONObject resourceJSON = new JSONObject(responseResource.body());
    JSONObject actualResource = resourceJSON.getJSONObject(individualItemKey);
    JSONArray array = new JSONArray();
    array.put(zohoInvoicesJsonProcessing(actualResource, organizationId));
    array.forEach(
        ob -> {
          JSONObject obj = ((JSONObject) ob);
          log.info(
              " successful processing for zoho {} for org {} id {} line-item-id {} ",
              type,
              organizationId,
              obj.getString(stringIdLookupMap.get(type)),
              obj.getString("line_item_id"));
        });
    return array;
  }

  @Override
  public JSONArray process(String s) throws Exception {
    return processAdapter(s);
  }
}
