package com.pearrity.mantys.chargebee.writer;

import com.pearrity.mantys.domain.EtlMetadata;
import com.pearrity.mantys.utils.UtilFunctions;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.pearrity.mantys.chargebee.ChargeBeeResourceUtil.*;
import static com.pearrity.mantys.utils.UtilFunctions.getEtlMetadataByDomainAndPlatform;

public class ChargebeeJdbcWriter implements ItemWriter<JSONArray> {
  private final Logger log = Logger.getLogger(ChargebeeJdbcWriter.class.getName());
  private final String type;
  private final String tenant;
  private final EtlMetadata etlMetadata;

  private final EtlMetadata lineItemEtlMetadata;

  public ChargebeeJdbcWriter(String type, String tenant) {
    this.type = type;
    this.tenant = tenant;
    this.etlMetadata = getEtlMetadataByDomainAndPlatform(type, tenant, "chargebee");
    lineItemEtlMetadata =
        this.type.equalsIgnoreCase(Invoices) || this.type.equalsIgnoreCase(CreditNotes)
            ? getEtlMetadataByDomainAndPlatform(LineItem, tenant, "chargebee")
            : null;
  }

  @Override
  public void write(@NonNull List<? extends JSONArray> list) {
    write(list, etlMetadata, type);
  }

  public void write(List<? extends JSONArray> list, EtlMetadata etlMetadata, String type) {
    JdbcTemplate jdbcTemplate = UtilFunctions.getJdbcTemplateByTenant(tenant);
    String insertQuery = etlMetadata.getInsertQuery();
    String[] keys = etlMetadata.getJsonKeys().split(",");
    String typeList = Strings.repeat("12,", keys.length);
    int[] types =
        Arrays.stream(typeList.substring(0, typeList.length() - 1).split(","))
            .mapToInt(Integer::parseInt)
            .toArray();
    List<Object[]> arrList = new ArrayList<>();
    for (JSONArray i : list) {
      for (Object ob : i) {
        JSONObject t = (JSONObject) ob;
        if (t.has("id")) log.info("id for " + type + " " + t.getString("id"));
        Object[] arr = writeEntity(jdbcTemplate, t, keys, type);
        if (type.equalsIgnoreCase(Invoices) || type.equalsIgnoreCase(CreditNotes)) {
          if (t.has(LineItem)) {
            jdbcTemplate.update(
                """
            update chargebee_line_items set tt_end = to_timestamp(?::bigint) at time zone 'UTC'
            where parent_id = ?::text and tt_end is null and line_item_for = ?::text;
            """,
                t.get(updatedAt),
                t.getString("id"),
                type);
            write(List.of(t.getJSONArray(LineItem)), lineItemEtlMetadata, LineItem);
          }
        }
        if (arr != null) arrList.add(arr);
      }
    }
    try {
      jdbcTemplate.batchUpdate(insertQuery, arrList, types);
      log.info("successfully written " + arrList.size() + " " + type + "s");
    } catch (Exception e) {
      log.log(
          Level.SEVERE, "error during insertion", e.getMessage() + " " + e.getCause().getMessage());
    }
  }

  private Object[] writeEntity(
      JdbcTemplate jdbcTemplate, JSONObject t, String[] keys, String type) {
    try {
      if (Objects.equals(type, LineItem)) {
        t.put("line_item_for", this.type);
      }
      JSONObject cleanJson = new JSONObject(t.toString());
      try {
        cleanJson.remove("pdf");
        cleanJson.remove("file");
      } catch (Exception e) {
        log.log(Level.SEVERE, e.getMessage());
      }
      if (!t.has(updatedAt)) {
        log.info(updatedAt + " not found for id " + (t.has("id") ? t.getString("id") : ""));
        //      TODO : add that id to unsuccessful sync
        return null;
      }
      t.put("tt_begin", t.get(updatedAt));
      t.put("json", cleanJson.toString());
      if (!type.equalsIgnoreCase(LineItem)) {
        String updateQuery =
            "update chargebee_"
                + type
                + " set tt_end = to_timestamp(?::bigint) at time zone 'UTC' where Id::text ="
                + " ?::text  and tt_end is null ";
        jdbcTemplate.update(updateQuery, t.get(updatedAt), t.getString("id"));
      } else {
        String updateQuery =
            """
                update chargebee_line_items set tt_end = to_timestamp(?::bigint) at time zone 'UTC'
            where parent_id = ?::text and tt_end is null and line_item_for = ?::text and id = ?::text;
                """;
        jdbcTemplate.update(
            updateQuery, t.get(updatedAt), t.getString("parent_id"), this.type, t.getString("id"));
      }
      Object[] arr = new Object[keys.length];
      for (int ii = 0; ii < keys.length; ii++) {
        try {
          arr[ii] = t.get(keys[ii].trim());
        } catch (JSONException e) {
          arr[ii] = null;
        }
      }
      return arr;
    } catch (Exception e) {
      log.log(Level.SEVERE, e.getMessage());
      if (e.getCause() != null) log.log(Level.SEVERE, e.getCause().getMessage());
      return null;
    }
  }
}
