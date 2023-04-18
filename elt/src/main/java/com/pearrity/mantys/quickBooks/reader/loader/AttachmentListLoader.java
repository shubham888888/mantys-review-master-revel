package com.pearrity.mantys.quickBooks.reader.loader;

import com.pearrity.mantys.interfaces.EtlLoader;
import com.pearrity.mantys.quickBooks.QuickBooksAuthUtil;
import com.pearrity.mantys.repository.config.DbConfiguration;
import com.pearrity.mantys.repository.config.SpringContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.pearrity.mantys.quickBooks.QuickBooksResourceUtil.*;

public class AttachmentListLoader implements EtlLoader {

  private final QuickBooksAuthUtil quickBooksAuthUtil;

  private final String type;
  private final String realmId;

  private final String tenant;

  public AttachmentListLoader(QuickBooksAuthUtil authUtil, String type, String realmId) {
    this.tenant = authUtil.getTenant();
    this.quickBooksAuthUtil = authUtil;
    this.type = type;
    this.realmId = realmId;
  }

  @Override
  public List<String> load(Timestamp lastSyncTime) throws Exception {
    String lastSyncDate = "2022-01-01";
    if (lastSyncTime != null) {
      lastSyncDate = new SimpleDateFormat("yyyy-MM-dd").format(Date.from(lastSyncTime.toInstant()));
    }
    JdbcTemplate jdbcTemplate =
        SpringContext.getBean(DbConfiguration.class).getJdbcTemplateByDomain(tenant);
    String loaderQuery =
        """
            select val From
            (SELECT concat(A.VALUE ,'-',B.VALUE) as val
                FROM PROFIT_LOSS_DETAIL_REPORT A
                INNER JOIN
                (SELECT *
                        FROM PROFIT_LOSS_DETAIL_REPORT
                WHERE KEY2 = 'type_id'
                and realm_id like :realmId
                AND VALUE IS NOT NULL) B ON A.UUID = B.UUID
                WHERE A.KEY2 = 'Transaction Type'
                AND a.value not in ('','Invoice')
                AND B.VALUE != ''
                order by a.value asc) k
                where k.val not in (select concat(type, '-' , type_id) from attachables where realm_id like :realmId);
            """;
    List<String> list =
        new NamedParameterJdbcTemplate(jdbcTemplate)
            .query(
                loaderQuery,
                Map.of("realmId", realmId),
                rs -> {
                  List<String> strings = new ArrayList<>();
                  while (rs.next()) {
                    strings.add(rs.getString(1));
                  }
                  return strings;
                });
    List<String> attachmentsList = new ArrayList<>();
    for (int i = 0; i < Objects.requireNonNull(list).size(); ) {
      String currentType = list.get(i).split("-")[0];
      StringBuilder listOfIds = new StringBuilder();
      int count = 0;
      while (i < list.size()
          && Objects.equals(list.get(i).split("-")[0], currentType)
          && count <= 30) {
        if (!listOfIds.isEmpty()) listOfIds.append(",");
        listOfIds.append("'").append(list.get(i).split("-")[1]).append("'");
        i++;
        count++;
      }
      String intuitQuery =
          """
              select Id from attachable where AttachableRef.EntityRef.Type = '%s'
              and AttachableRef.EntityRef.value in (%s)
              and MetaData.LastUpdatedTime >= '%s'
                                          """;
      String intuitQuery2 = intuitQuery.formatted(currentType, listOfIds.toString(), lastSyncDate);
      Map<String, Object> res =
          executeQuery(
              intuitQuery2, quickBooksAuthUtil, realmId, type.substring(0, type.length() - 1));
      JSONArray array = (JSONArray) res.get("array");
      for (Object o : array) {
        JSONObject ob = (JSONObject) o;
        attachmentsList.add(ob.getString("Id"));
      }
    }
    return attachmentsList;
  }
}
