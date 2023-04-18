package com.pearrity.mantys.hubspot.reader.loader;

import com.pearrity.mantys.hubspot.HubSpotAuthUtil;
import com.pearrity.mantys.hubspot.HubSpotResourceUtil;
import com.pearrity.mantys.interfaces.EtlLoader;
import com.pearrity.mantys.utils.UtilFunctions;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Logger;

import static com.pearrity.mantys.hubspot.HubSpotResourceUtil.*;

public class HubspotEntityLoader implements EtlLoader {

  private final Logger log = Logger.getLogger(HubspotEntityLoader.class.getName());
  private final HubSpotAuthUtil hubSpotAuthUtil;
  private final String type;

  public HubspotEntityLoader(HubSpotAuthUtil quickBooksAuthUtil, String type) {
    this.hubSpotAuthUtil = quickBooksAuthUtil;
    this.type = type;
  }

  @Override
  public List<String> load(Timestamp lastSyncTime) throws Exception {
    Set<String> list = new HashSet<>();
    String params = "?limit=75";
    String after = "0";
    Map<String, Object> res;
    do {
      if (lastSyncTime == null || Objects.equals(type, Owners)) {
        res = HubSpotResourceUtil.executeEntityRequest(hubSpotAuthUtil, type, params, false);
      } else {
        res =
            HubSpotResourceUtil.executeSearchRequest(
                hubSpotAuthUtil, type, after, String.valueOf(lastSyncTime.getTime()));
      }
      after = (String) res.get("repeat");
      params = "?limit=75&after=%s".formatted(res.get("repeat"));
      JSONArray array = ((JSONArray) res.get("array"));
      for (Object ob : array) {
        JSONObject obj = (JSONObject) ob;
        boolean insert =
            lastSyncTime == null
                || checkIfToInsertIntoDB(
                    hubSpotAuthUtil,
                    type,
                    obj.getString("id"),
                    obj.has(updatedAt) ? obj.getString(updatedAt) : obj.getString(createdAt));
        if (insert) list.add(obj.getString("id"));
      }
    } while (!res.get("repeat").equals(Strings.EMPTY));
    return list.stream().toList();
  }

  private boolean checkIfToInsertIntoDB(
      HubSpotAuthUtil hubSpotAuthUtil, String type, String id, String lastModifiedDate) {
    log.info("type : " + type + " & id :" + id);
    String query =
        "select count(*)  > 0 from hubspot_"
            + type
            + " where id = ?::text and last_modified_time = ?::timestamp";
    try {
      return Boolean.FALSE.equals(
          UtilFunctions.getJdbcTemplateByTenant(hubSpotAuthUtil.getTenant())
              .queryForObject(query, Boolean.class, id, lastModifiedDate));
    } catch (Exception e) {
      return true;
    }
  }
}
