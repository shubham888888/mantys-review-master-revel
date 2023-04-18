package com.pearrity.mantys.salesforce.writer;

import com.pearrity.mantys.domain.EtlMetadata;
import com.pearrity.mantys.salesforce.SalesforceResourceUtil;
import com.pearrity.mantys.utils.UtilFunctions;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import static com.pearrity.mantys.domain.utils.Constants.*;
import static com.pearrity.mantys.salesforce.SalesforceResourceUtil.*;
import static com.pearrity.mantys.utils.UtilFunctions.*;

@Slf4j
public class SalesForceJdbcWriter implements ItemWriter<JSONArray> {

  private final String type;
  private final String domain;
  private final EtlMetadata etlMetadata;

  public SalesForceJdbcWriter(String type, String domain) {
    this.type = type;
    this.domain = domain;
    this.etlMetadata =
        UtilFunctions.getEtlMetadataByDomainAndPlatform(
            type, domain, SalesforceResourceUtil.platform);
  }

  @Override
  public void write(List<? extends JSONArray> list) {
    JdbcTemplate jdbcTemplate = UtilFunctions.getJdbcTemplateByTenant(domain);
    String insertQuery = etlMetadata.getInsertQuery();
    String[] keys = etlMetadata.getJsonKeys().split(",");
    int[] types = Arrays.stream(keys).mapToInt(a -> 12).toArray();
    int count = 0;
    int fail = 0;
    for (JSONArray jsonArrayList : list) {
      for (Object ob : jsonArrayList) {
        JSONObject t = (JSONObject) ob;
        if (t == null) {
          continue;
        }
        t.put("json", t.toString());
        t.put("tt_begin", Instant.now().atZone(ZoneId.of(UTC)).toInstant().toString());
        String updateQuery =
            String.format(
                "update %s set tt_end = ?::timestamp where id = ? and tt_end is null",
                "salesforce_" + type);
        jdbcTemplate.update(updateQuery, t.get("tt_begin"), t.getString("Id"));
        Object[] arr = new Object[keys.length];
        for (int i = 0; i < keys.length; i++) {
          try {
            arr[i] = t.get(keys[i].trim());
            if (arr[i] instanceof String && nullValue.equals(arr[i])) {
              arr[i] = null;
            }
          } catch (JSONException e) {
            arr[i] = null;
          }
        }
        try {
          jdbcTemplate.update(insertQuery, arr, types);
          count++;
        } catch (Exception e) {
          fail++;
          log.error("error during insertion for type {} id {}", type, t.getString("Id"), e);
          addToUnsuccessfulSync(t.getString("Id"), type + " entity", domain, platform, null);
        }
      }
    }
    log.error(" {} {} failed  while writing to db ", fail, type);
    log.info("successfully written {} {}  to db...", count, type);
  }
}
