package com.pearrity.mantys.quickBooks.reader.loader;

import com.pearrity.mantys.interfaces.EtlLoader;
import com.pearrity.mantys.quickBooks.QuickBooksAuthUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.pearrity.mantys.quickBooks.QuickBooksResourceUtil.executeQuery;

public class QBEntityLoader implements EtlLoader {
  private final QuickBooksAuthUtil quickBooksAuthUtil;

  private final String realmId;
  private final String type;

  public QBEntityLoader(QuickBooksAuthUtil quickBooksAuthUtil, String realmId, String type) {
    this.quickBooksAuthUtil = quickBooksAuthUtil;
    this.realmId = realmId;
    this.type = type;
  }

  /** select Id from Invoice startPosition 1 maxResults 10 stop loop if totalCount < maxResults */
  @Override
  public List<String> load(Timestamp lastSyncTime) throws Exception {
    Set<String> list = new HashSet<>();
    int maxResults = 30;
    boolean loop;
    int count = 0;
    String lastSyncDate = "2022-01-01";
    if (lastSyncTime != null) {
      lastSyncDate = new SimpleDateFormat("yyyy-MM-dd").format(Date.from(lastSyncTime.toInstant()));
    }
    Instant lastTimeCovered = Instant.now();
    do {
      String body =
          String.format(
              """
                          select
                              Id
                          from
                              %s
                              where MetaData.LastUpdatedTime >= '%s'
                              and MetaData.LastUpdatedTime <= '%s'
                              startPosition %d maxResults %d
                          """,
              type, lastSyncDate, lastTimeCovered.toString(), (count * maxResults), maxResults);
      Map<String, Object> res = executeQuery(body, quickBooksAuthUtil, realmId, type);
      JSONArray array = (JSONArray) res.get("array");
      loop = (boolean) res.get("repeat");
      for (Object o : array) {
        JSONObject ob = (JSONObject) o;
        list.add((String) ob.get("Id"));
      }
      count++;
    } while (loop);
    return list.stream().toList();
  }
}
