package com.pearrity.mantys.zoho;

import com.google.gson.Gson;
import com.pearrity.mantys.domain.OAuthSecret;
import com.pearrity.mantys.domain.ZohoCreditNotes;
import com.pearrity.mantys.domain.ZohoInvoice;
import com.pearrity.mantys.repository.config.AwsSecretsService;
import com.pearrity.mantys.repository.config.SpringContext;
import com.pearrity.mantys.zoho.processor.ZohoInvoiceProcessor;
import com.pearrity.mantys.zoho.writer.ZohoInvoiceDatabaseWriter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.pearrity.mantys.utils.UtilFunctions.makeWebRequest;

@Slf4j
public class ZohoInvoiceResourceUtil<T> {

  public static final Map<Class<?>, String> endpointLookupMap =
      Map.of(ZohoInvoice.class, "invoices", ZohoCreditNotes.class, "creditnotes");

  public static final Map<Class<?>, String> idLookupMap =
      Map.of(ZohoInvoice.class, "invoice_id", ZohoCreditNotes.class, "creditnote_id");

  public static final String invoice = "Invoice";

  public static final String creditNote = "CreditNote";

  public static final Map<String, String> stringEndpointLookupMap =
      Map.of(invoice, "invoices", creditNote, "creditnotes");

  public static final Map<String, String> stringIdLookupMap =
      Map.of(invoice, "invoice_id", creditNote, "creditnote_id");

  public static final String platform = "zoho";

  public static String getEndPoint(Class<?> t) {
    return endpointLookupMap.get(t);
  }

  public List<T> loadResourcesIntoList(
      String baseUrl,
      ZohoInvoiceAuthUtil authUtil,
      String key,
      Class<?> type,
      String organizationId)
      throws IOException, InterruptedException {
    int page = 0;
    List<T> list = new ArrayList<>();
    boolean nextPage = true;
    String endpoint = getEndPoint(type);
    String individualItemKey = endpoint.substring(0, endpoint.length() - 1);
    do {
      String url = String.format(baseUrl + "?per_page=200&page=%d", page);
      HttpResponse<String> response =
          (HttpResponse<String>)
              makeWebRequest(
                  createRequest(url, authUtil.getAccessToken(), organizationId), null, 0);
      JSONObject object = new JSONObject(response.body());
      if (checkResponseStatusAndAccessTokenReset(authUtil, response)) continue;
      JSONArray array = object.getJSONArray(key);

      for (int controlVar = 0; controlVar < array.length(); ) {

        JSONObject obj1 = (JSONObject) array.get(controlVar);

        // TODO REMOVE THE DATE FILTER eventually, added for API limit
        try {
          SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
          Date resourceDate = dateFormat.parse(obj1.getString("date"));
          Date checkDate = dateFormat.parse("2022-01-01");
          if (resourceDate.before(checkDate)) {
            controlVar++;
            continue;
          }
        } catch (Exception e) {
          log.info("date parse exception");
        }

        String urlResource = String.format(baseUrl + "/" + obj1.getString(idLookupMap.get(type)));
        HttpResponse<String> responseResource =
            (HttpResponse<String>)
                makeWebRequest(
                    createRequest(urlResource, authUtil.getAccessToken(), organizationId), null, 0);
        if (checkResponseStatusAndAccessTokenReset(authUtil, responseResource)) continue;

        JSONObject resourceJSON = new JSONObject(responseResource.body());
        JSONObject actualResource = resourceJSON.getJSONObject(individualItemKey);

        List<JSONObject> jsonObjects = zohoInvoicesJsonProcessing(actualResource, organizationId);

        Gson gson = new Gson();
        // TODO remove these once the ELT cleanup is done
        ZohoInvoiceProcessor<T> zohoInvoiceProcessor = new ZohoInvoiceProcessor<>(type, "locus.sh");
        ZohoInvoiceDatabaseWriter<T> zohoInvoiceDatabaseWriter =
            new ZohoInvoiceDatabaseWriter<>(type, "locus.sh");
        for (JSONObject obj : jsonObjects) {
          T resource = (T) gson.fromJson(obj.toString(), type);
          try {
            if (zohoInvoiceProcessor.getItemProcessor().process(resource) != null) {
              zohoInvoiceDatabaseWriter.getZohoInvoiceItemWriter().write(List.of(resource));
            }
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
          //          list.add(resource);
        }

        controlVar++;
      }
      if (object.getJSONObject("page_context").getBoolean("has_more_page")) {
        page++;
      } else {
        nextPage = false;
      }
    } while (nextPage);
    return list;
  }

  public static HttpRequest createRequest(
      String urlResource, String access_token, String organizationId) {
    return HttpRequest.newBuilder()
        .uri(URI.create(urlResource))
        .header("Content-Type", "text/plain; charset=UTF-8")
        .header("Authorization", "Zoho-oauthtoken " + access_token)
        .header("X-com-zoho-invoice-organizationid", organizationId)
        .GET()
        .build();
  }

  public static boolean checkResponseStatusAndAccessTokenReset(
      ZohoInvoiceAuthUtil authUtil, HttpResponse<String> response)
      throws IOException, InterruptedException {
    if (response.statusCode() != 200 && response.statusCode() != 401)
      throw new RuntimeException("error processing request");
    if (response.statusCode() == 401) {
      authUtil.setAccessToken();
      return true;
    }
    return false;
  }

  public static List<JSONObject> zohoInvoicesJsonProcessing(
      JSONObject jsonObject, String organizationId) {
    JSONArray array = jsonObject.getJSONArray("line_items");
    List<JSONObject> jsonObjects = new ArrayList<>();
    for (Object ob : array) {
      JSONObject lineItemJson = (JSONObject) ob;
      JSONObject obj = new JSONObject(jsonObject, jsonObject.keySet().toArray(new String[0]));
      obj.put("total", lineItemJson.get("item_total"));
      String desc = null;
      try {
        obj.put("line_item_id", lineItemJson.getString("line_item_id"));
        desc = lineItemJson.getString("description");
        obj.put("item_description", desc);
      } catch (Exception e) {
        log.error("Error occurred " + e.getMessage());
      }
      setRevenueTypeFromLineItems(obj, desc);
      obj.put("organization_id", organizationId);
      //      obj.put("json", obj.toString());
      jsonObjects.add(obj);
    }
    return jsonObjects;
  }

  private static void setRevenueTypeFromLineItems(JSONObject obj, String desc) {
    if (desc == null) obj.put("recurring", "recurring");
    else if ((desc.toLowerCase().contains("poc")
        || desc.toLowerCase().contains("trial period")
        || desc.toLowerCase().contains("integration")
        || desc.toLowerCase().contains("set up fees")
        || desc.toLowerCase().contains("one time set up")
        || desc.toLowerCase().contains("professional services")
        || desc.toLowerCase().contains("onetime"))) obj.put("recurring", "Non-recurring");
    else if (desc.toLowerCase().contains("late fees")
        || desc.toLowerCase().contains("finance charges")) obj.put("recurring", "Other income");
    else obj.put("recurring", "Recurring");
  }

  public static ZohoInvoiceAuthUtil getAuthUtilBYDomain(String domain)
      throws IOException, InterruptedException {
    String key = domain + ".zoho";
    OAuthSecret oauthsecret =
        SpringContext.getBean(AwsSecretsService.class).getSecret(key, OAuthSecret.class);
    if (oauthsecret == null) throw new RuntimeException("problem getting OAuthSecret for " + key);
    return new ZohoInvoiceAuthUtil(
        oauthsecret.getLoginBaseUrl(),
        oauthsecret.getResourceBaseUrl(),
        oauthsecret.getRefreshToken(),
        oauthsecret.getClientSecret(),
        oauthsecret.getClientId(),
        oauthsecret.getRedirectUri());
  }
}
