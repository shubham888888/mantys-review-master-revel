package com.pearrity.mantys.hubspot.writer;

import com.pearrity.mantys.domain.EtlMetadata;
import com.pearrity.mantys.utils.UtilFunctions;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.pearrity.mantys.hubspot.HubSpotResourceUtil.*;
import static com.pearrity.mantys.utils.UtilFunctions.*;

public class HubSpotJdbcWriter implements ItemWriter<JSONArray> {
  private final Logger log = Logger.getLogger(HubSpotJdbcWriter.class.getName());
  private final String type;
  private final String tenant;
  private final EtlMetadata etlMetadata;

  public HubSpotJdbcWriter(String type, String tenant) {
    this.type = type;
    this.tenant = tenant;
    this.etlMetadata = getEtlMetadataByDomainAndPlatform(type, tenant, "hubspot");
  }

  @Override
  public void write(List<? extends JSONArray> list) {
    List<String> idsListToWriteInDB = new ArrayList<>();
    JdbcTemplate jdbcTemplate = UtilFunctions.getJdbcTemplateByTenant(tenant);
    String insertQuery = etlMetadata.getInsertQuery();
    String[] keys = etlMetadata.getJsonKeys().split(",");
    String typeList = Strings.repeat("12,", keys.length);
    int[] types =
        Arrays.stream(typeList.substring(0, typeList.length() - 1).split(","))
            .mapToInt(Integer::parseInt)
            .toArray();
    List<Object[]> arrList = new ArrayList<>();
    for (JSONArray arrayElement : list) {
      //      TODO: rewrite names
      for (Object ob : arrayElement) {
        JSONObject t = (JSONObject) ob;
        idsListToWriteInDB.add(t.has("id") ? t.getString("id") : null);
        Object[] arr = writeEntity(jdbcTemplate, t, keys);
        if (arr != null) arrList.add(arr);
      }
    }
    try {
      jdbcTemplate.batchUpdate(insertQuery, arrList, types);
      log.info("successfully written " + arrList.size() + " " + type + "s");
    } catch (Exception e) {
      log.log(Level.SEVERE, idsListToWriteInDB.toString());
      idsListToWriteInDB.forEach(
          id -> UtilFunctions.addToUnsuccessfulSync(id, tenant, type, platform, null));
      log.log(
          Level.SEVERE,
          "error during insertion "
              + e.getMessage()
              + " "
              + ((e.getCause() != null) ? e.getCause().getMessage() : Strings.EMPTY));
    }
  }

  private Object[] writeEntity(JdbcTemplate jdbcTemplate, JSONObject t, String[] keys) {
    JSONObject cleanJson = new JSONObject(t.toString());
    try {
      cleanJson.remove("pdf");
      cleanJson.remove("file");
    } catch (Exception e) {
      log.log(Level.SEVERE, e.getMessage());
    }
    t.put("tt_begin", t.has(updatedAt) ? t.getString(updatedAt) : t.getString(createdAt));
    t.put("json", cleanJson.toString());
    String updateQuery =
        "update hubspot_"
            + type
            + " set tt_end = ?::timestamp where Id::text = ?::text"
            + " and tt_end is null";
    try {

      jdbcTemplate.update(
          updateQuery,
          t.has(updatedAt) ? t.getString(updatedAt) : t.getString(createdAt),
          t.getString("id"));
    } catch (Exception e) {
      log.log(Level.SEVERE, e.getMessage());
      jdbcTemplate.update(
          "insert into etl_unsuccessful_sync (domain , id , platform ) values (? , ? , ?)",
          type,
          t.getString("id"),
          "hubspot");
      return null;
    }
    //    TODO: rewrite names
    Object[] arr = new Object[keys.length];
    for (int ii = 0; ii < keys.length; ii++) {
      try {
        arr[ii] = t.get(keys[ii].trim());
      } catch (JSONException e) {
        arr[ii] = null;
      }
    }
    return arr;
  }
}
