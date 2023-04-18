package com.pearrity.mantys.quickBooks.writer;

import com.pearrity.mantys.domain.EtlMetadata;
import com.pearrity.mantys.quickBooks.QuickBooksResourceUtil;
import com.pearrity.mantys.utils.UtilFunctions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.pearrity.mantys.utils.UtilFunctions.getEtlMetadataByDomainAndPlatform;

public class QuickBooksJdbcWriter implements ItemWriter<JSONArray> {
  private final Logger log = Logger.getLogger(QuickBooksJdbcWriter.class.getName());
  private final String type;
  private final String tenant;
  private final String realmId;
  private final EtlMetadata etlMetadata;

  public QuickBooksJdbcWriter(String type, String tenant, String realmId) {
    this.tenant = tenant;
    this.type =
        type.contains(QuickBooksResourceUtil.Attachable) ? QuickBooksResourceUtil.Attachable : type;
    this.realmId = realmId;
    this.etlMetadata =
        getEtlMetadataByDomainAndPlatform(this.type, tenant, QuickBooksResourceUtil.platform);
  }

  private void updateTtEndForLastSyncs(
      String key1, String key2, String fullKey, String vtBegin, String vtEnd) {
    JdbcTemplate jdbcTemplate = UtilFunctions.getJdbcTemplateByTenant(tenant);
    String updateQuery =
        """
                delete
                    from  %s
                where
                    realm_id = ?::text
                    and key1 = ?::text
                    and key2 = ?::text
                    and full_key1 = ?::text
                    and vt_begin = ?::timestamp
                    and vt_end = ?::timestamp
                    and tt_end is null
                """
            .formatted(
                (Objects.equals(type, QuickBooksResourceUtil.ProfitAndLossDetail)
                    ? "profit_loss_detail_report"
                    : "profit_loss_report"));
    try {
      int m = jdbcTemplate.update(updateQuery, realmId, key1, key2, fullKey, vtBegin, vtEnd);
      log.info(m + " rows updated in " + type);
    } catch (Exception e) {
      log.log(Level.SEVERE, "ERROR : " + e);
    }
  }

  @Override
  public void write(List<? extends JSONArray> list) {
    JdbcTemplate jdbcTemplate = UtilFunctions.getJdbcTemplateByTenant(tenant);
    String insertQuery = etlMetadata.getInsertQuery();
    int[] types =
        Arrays.stream(etlMetadata.getSqlTypes().split(",")).mapToInt(Integer::parseInt).toArray();
    String[] keys = etlMetadata.getJsonKeys().split(",");
    List<Object[]> arrList = new ArrayList<>();
    for (JSONArray i : list) {
      for (Object ob : i) {
        JSONObject t = (JSONObject) ob;
        Object[] arr;
        if (Objects.equals(type, QuickBooksResourceUtil.ProfitAndLossReport)
            || Objects.equals(type, QuickBooksResourceUtil.ProfitAndLossDetail)) {
          arr = writeProfitAndLossReportEntities(t, realmId, keys);
        } else {
          arr = writeEntity(jdbcTemplate, t, realmId, keys);
        }
        if (arr != null) arrList.add(arr);
      }
    }
    try {
      for (int i = 0; i < arrList.size(); i += 50) {
        List<Object[]> lists = new ArrayList<>();
        for (int j = 0; j < 50 && i + j < arrList.size(); j++) {
          lists.add(arrList.get(i + j));
        }
        jdbcTemplate.batchUpdate(insertQuery, lists, types);
        System.out.println("successfully written " + lists.size() + " " + type + "s");
      }
      System.out.println("successfully written total " + arrList.size() + " " + type + "s");
    } catch (Exception e) {
      log.log(
          Level.SEVERE, "error during insertion", e.getMessage() + " " + e.getCause().getMessage());
    }
  }

  private Object[] writeProfitAndLossReportEntities(JSONObject t, String realmId, String[] keys) {
    t.put("tt_begin", t.get("last_modified_time"));
    t.put("realm_id", realmId);
    t.put("json", t.toString());
    if (Objects.equals(type, QuickBooksResourceUtil.ProfitAndLossReport)) {
      try {
        String value = t.getString("value");
        double v = Double.parseDouble(value);
      } catch (Exception e) {
        t.put("value", "0.0");
      }
    }
    Object[] arr = new Object[keys.length];
    for (int ii = 0; ii < keys.length; ii++) {
      try {
        arr[ii] = t.get(keys[ii].trim());
      } catch (JSONException e) {
        arr[ii] = null;
      }
    }
    updateTtEndForLastSyncs(
        t.getString("key1"),
        t.getString("key2"),
        t.getString("full_key1"),
        t.getString("vt_begin"),
        t.getString("vt_end"));
    return arr;
  }

  private Object[] writeEntity(
      JdbcTemplate jdbcTemplate, JSONObject t, String realmId, String[] keys) {
    JSONObject cleanJson = new JSONObject(t.toString());
    try {
      cleanJson.remove("pdf");
      cleanJson.remove("file");
    } catch (Exception e) {
      log.log(Level.SEVERE, e.getMessage());
    }
    t.put("tt_begin", t.get("last_modified_time"));
    t.put("realm_id", realmId);
    t.put("json", cleanJson.toString());
    String countQuery =
        """
            select count(*) > 0 from %s
            where Id::text = ?::text and realm_id::text = ?::text and last_modified_time = ?::timestamp
             and tt_end is null
            """
            .formatted(type + "s");
    boolean b =
        Boolean.TRUE.equals(
            jdbcTemplate.queryForObject(
                countQuery,
                Boolean.class,
                t.getString("Id"),
                realmId,
                t.getString("last_modified_time")));
    if (b) return null;
    String updateQuery =
        """
            update
            %s set tt_end = ?::timestamp where Id::text = ?::text and realm_id::text = ?::text and last_modified_time < ?::timestamp
             and tt_end is null
            """
            .formatted(type + "s");
    int k =
        jdbcTemplate.update(
            updateQuery,
            t.getString("last_modified_time"),
            t.getString("Id"),
            realmId,
            t.getString("last_modified_time"));
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
