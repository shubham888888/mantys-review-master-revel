package com.pearrity.mantys.zohoV2.reader;

import com.pearrity.mantys.repository.config.DbConfiguration;
import com.pearrity.mantys.repository.config.SpringContext;
import com.pearrity.mantys.zoho.ZohoInvoiceAuthUtil;
import com.pearrity.mantys.zoho.ZohoInvoiceResourceUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import static com.pearrity.mantys.utils.UtilFunctions.makeWebRequest;
import static com.pearrity.mantys.zoho.ZohoInvoiceResourceUtil.*;

public class ZohoApiReader {

  public static ListItemReader<String> getZohoItemReader(
      String type, String domain, String organizationId, ZohoInvoiceAuthUtil zohoInvoiceAuthUtil)
      throws IOException, InterruptedException {
    JdbcTemplate jdbcTemplate =
        SpringContext.getBean(DbConfiguration.class).getJdbcTemplateByDomain(domain);
    List<String> zohoItems = loadResources(zohoInvoiceAuthUtil, type, organizationId, jdbcTemplate);
    return new ListItemReader<>(zohoItems);
  }

  private static List<String> loadResources(
      ZohoInvoiceAuthUtil authUtil, String type, String organizationId, JdbcTemplate jdbcTemplate)
      throws IOException, InterruptedException {

    String endpoint = "/api/v3/" + ZohoInvoiceResourceUtil.stringEndpointLookupMap.get(type);
    String baseUrl = String.format("%s%s", authUtil.resourceBaseUrl, endpoint);
    int page = 0;
    List<String> list = new ArrayList<>();
    boolean nextPage = true;
    String key = stringEndpointLookupMap.get(type);
    do {
      String url = String.format(baseUrl + "?per_page=200&page=%d", page);
      HttpResponse<String> response =
          (HttpResponse<String>)
              makeWebRequest(
                  createRequest(url, authUtil.getAccessToken(), organizationId), null, 0);
      if (checkResponseStatusAndAccessTokenReset(authUtil, response)) continue;
      JSONObject object = new JSONObject(response.body());
      JSONArray array = object.getJSONArray(key);
      for (int i = 0; i < array.length(); i++) {
        JSONObject jsonObject = array.getJSONObject(i);
        boolean checkIfToInsertIntoDB =
            checkIfToInsertIntoDB(
                jsonObject.getString(stringIdLookupMap.get(type)),
                jsonObject.getString("last_modified_time"),
                jdbcTemplate,
                type,
                organizationId);
        if (checkIfToInsertIntoDB) {
          list.add(jsonObject.getString(stringIdLookupMap.get(type)));
        }
      }
      if (object.getJSONObject("page_context").getBoolean("has_more_page")) {
        page++;
      } else {
        nextPage = false;
      }
    } while (nextPage);
    return list;
  }

  private static boolean checkIfToInsertIntoDB(
      String id,
      String lastModifiedTime,
      JdbcTemplate jdbcTemplate,
      String type,
      String organizationId) {
    {
      String query =
          String.format(
              "select (count(*) > 0) as res from  %s where %s = ? and last_modified_time <"
                  + " ?::timestamp and organization_id = ? and tt_end is null",
              stringEndpointLookupMap.get(type), stringIdLookupMap.get(type));
      Boolean res =
          jdbcTemplate.queryForObject(query, Boolean.class, id, lastModifiedTime, organizationId);
      if (Boolean.FALSE.equals(res)) {
        Boolean exists =
            jdbcTemplate.queryForObject(
                String.format(
                    "select count(*) > 0 from %s where %s = ? and  organization_id = ? and"
                        + " tt_end is null",
                    stringEndpointLookupMap.get(type), stringIdLookupMap.get(type)),
                Boolean.class,
                id,
                organizationId);
        return Boolean.FALSE.equals(exists);
      } else {
        return true;
      }
    }
  }
}
