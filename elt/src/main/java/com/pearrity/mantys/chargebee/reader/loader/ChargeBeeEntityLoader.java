package com.pearrity.mantys.chargebee.reader.loader;

import com.pearrity.mantys.chargebee.ChargeBeeAuthUtil;
import com.pearrity.mantys.chargebee.ChargeBeeResourceUtil;
import com.pearrity.mantys.interfaces.EtlLoader;
import com.pearrity.mantys.utils.UtilFunctions;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChargeBeeEntityLoader implements EtlLoader {
  private final ChargeBeeAuthUtil chargeBeeAuthUtil;

  private final String type;

  public ChargeBeeEntityLoader(ChargeBeeAuthUtil chargeBeeAuthUtil, String type) {
    this.chargeBeeAuthUtil = chargeBeeAuthUtil;
    this.type = type;
  }

  public List<String> load(Timestamp lastSyncTime) throws Exception {
    Set<String> list = new HashSet<>();
    String lastSyncDate;
    if (lastSyncTime == null) {
      lastSyncDate =
          String.valueOf(new SimpleDateFormat("yyyy-MM-dd").parse("2022-01-01").getTime() / 1000);
    } else {
      lastSyncDate = String.valueOf(lastSyncTime.getTime() / 1000);
    }
    String sortBy =
        (Objects.equals(type, ChargeBeeResourceUtil.CreditNotes) ? "date" : "updated_at");
    String param = "limit=20&updated_at[after]=%s&sort_by[asc]=%s".formatted(lastSyncDate, sortBy);
    String offset;
    do {
      String url = chargeBeeAuthUtil.resourceBaseUrl(type);
      Map<String, Object> map =
          ChargeBeeResourceUtil.executeEntityListRequest(chargeBeeAuthUtil, type, url, param);
      JSONArray array = (JSONArray) map.get("array");
      for (Object ob : array) {
        JSONObject jsonObject = (JSONObject) ob;
        if (neededToSync(jsonObject)) list.add(jsonObject.getString("id"));
      }
      offset = (String) map.get("next");
      param =
          "limit=20&updated_at[after]=%s&sort_by[asc]=%s&offset=%s"
              .formatted(lastSyncDate, sortBy, offset);
    } while (!offset.equals(Strings.EMPTY));
    return list.stream().toList();
  }

  private boolean neededToSync(JSONObject jsonObject) {
    return Boolean.TRUE.equals(
        UtilFunctions.getJdbcTemplateByTenant(chargeBeeAuthUtil.getTenant())
            .query(
                """
                    select count(*) < 1 from chargebee_%s
                      where id = ?::text
                      and last_modified_time = to_timestamp(?::bigint) at time zone 'UTC'
                    """
                    .formatted(type),
                rs -> (!rs.next()) || rs.getBoolean(1),
                jsonObject.get("id"),
                jsonObject.get(ChargeBeeResourceUtil.updatedAt)));
  }
}
