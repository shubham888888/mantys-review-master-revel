package com.pearrity.mantys.quickBooks.reader.loader;

import com.pearrity.mantys.interfaces.EtlLoader;
import com.pearrity.mantys.quickBooks.QuickBooksAuthUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;

import static com.pearrity.mantys.domain.utils.UtilFunctions.incrementMonth;
import static com.pearrity.mantys.quickBooks.QuickBooksResourceUtil.executeQuery;

public class QBlCustomerLoader implements EtlLoader {

  private static final Logger log = Logger.getLogger(QBlCustomerLoader.class.getName());
  private final QuickBooksAuthUtil quickBooksAuthUtil;

  private final String realmId;

  public QBlCustomerLoader(QuickBooksAuthUtil quickBooksAuthUtil, String realmId) {
    this.quickBooksAuthUtil = quickBooksAuthUtil;
    this.realmId = realmId;
  }

  /** date time format required '2014-12-12T14:50:22-08:00' */
  public List<String> loadCustomers(Timestamp lastSyncTime) throws Exception {
    Set<String> list = new HashSet<>();
    int maxResults = 30;
    boolean loop;
    int count = 0;
    do {
      String body =
          String.format(
              "select * from Customer startPosition %d maxResults %d ",
              (count * maxResults), maxResults);
      Map<String, Object> res = executeQuery(body, quickBooksAuthUtil, realmId, "Customer");
      JSONArray array = (JSONArray) res.get("array");
      loop = (boolean) res.get("repeat");
      for (Object o : array) {
        JSONObject ob = (JSONObject) o;
        list.add(ob.getString("Id"));
      }
      count++;
    } while (loop);
    return list.stream().toList();
  }

  @Override
  public List<String> load(Timestamp lastSyncTime) throws ParseException {
    List<String> strings = new ArrayList<>();
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    Date start = Date.from(dateFormat.parse("2022-01-01").toInstant());
    Date finalEnd = incrementMonth(new Date());
    for (; start.before(finalEnd); start = incrementMonth(start)) {
      StringBuilder sb = new StringBuilder();
      Date end = new Date(start.getTime());
      end = incrementMonth(end);
      end = Date.from(end.toInstant().minus(1, ChronoUnit.DAYS));
      sb.append(dateFormat.format(start)).append("--").append(dateFormat.format(end));
      strings.add(sb.toString());
    }
    return strings;
  }
}
