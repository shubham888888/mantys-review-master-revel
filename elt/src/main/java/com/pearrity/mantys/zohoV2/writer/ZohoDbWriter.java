package com.pearrity.mantys.zohoV2.writer;

import com.pearrity.mantys.domain.EtlMetadata;
import com.pearrity.mantys.utils.UtilFunctions;
import com.pearrity.mantys.zoho.ZohoInvoiceResourceUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class ZohoDbWriter implements ItemWriter<JSONArray> {

  private final Logger log = Logger.getLogger(ZohoDbWriter.class.getName());
  private final String type;
  private final String domain;
  private final String org;
  private final EtlMetadata etlMetadata;

  public ZohoDbWriter(String type, String org, String domain) {
    this.type = type;
    this.domain = domain;
    this.org = org;
    this.etlMetadata = UtilFunctions.getEtlMetadataByDomainAndPlatform(type, domain, "zoho");
  }
  /**
   * @param list
   */
  @Override
  public void write(List<? extends JSONArray> list) {
    JdbcTemplate jdbcTemplate = UtilFunctions.getJdbcTemplateByTenant(domain);
    String insertQuery = etlMetadata.getInsertQuery();
    int[] types =
        Arrays.stream(etlMetadata.getSqlTypes().split(",")).mapToInt(Integer::parseInt).toArray();
    List<Object[]> arrList = new ArrayList<>();
    String[] keys = etlMetadata.getJsonKeys().split(",");
    for (JSONArray i : list) {
      for (Object ob : i) {
        JSONObject t = (JSONObject) ob;
        t.put("tt_begin", t.get("last_modified_time"));
        t.put("organization_id", org);
        t.put("json", t.toString());
        String updateQuery =
            String.format(
                "update %s set tt_end = ?::timestamp where line_item_id = ? and tt_end is null",
                ZohoInvoiceResourceUtil.stringEndpointLookupMap.get(type));
        jdbcTemplate.update(
            updateQuery, t.getString("last_modified_time"), t.getString("line_item_id"));
        Object[] arr = new Object[keys.length];
        for (int ii = 0; ii < keys.length; ii++) {
          try {
            arr[ii] = t.get(keys[ii].trim());
          } catch (JSONException e) {
            arr[ii] = null;
          }
        }
        log.info("writing into db %s %s".formatted(insertQuery, arr));
        arrList.add(arr);
      }
    }
    jdbcTemplate.batchUpdate(insertQuery, arrList, types);
  }
}
