package com.pearrity.mantys.salesforce;

import com.pearrity.mantys.domain.utils.Constants;
import com.pearrity.mantys.interfaces.AuthUtil;
import com.pearrity.mantys.interfaces.RequestType;
import com.pearrity.mantys.repository.utils.ListData;
import com.pearrity.mantys.repository.utils.Utils;
import com.pearrity.mantys.utils.UtilFunctions;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.pearrity.mantys.domain.utils.Constants.*;
import static com.pearrity.mantys.repository.utils.Utils.*;
import static com.pearrity.mantys.utils.UtilFunctions.*;

@Slf4j
public class SalesforceResourceUtil {

  public static final String platform = "salesforce";
  public static final String SalesforceOpportunity = "Opportunity";
  public static final String SalesforceAccount = "Account";
  public static final String SalesforceUser = "User";
  public static final Map<String, String> resourceEndPointLookupMap =
      Map.of(
          SalesforceOpportunity,
          "Opportunity",
          SalesforceAccount,
          "Account",
          SalesforceUser,
          "User");
  public static final long IncrementInDays = 30;
  public static ListData attributesMap =
      (tenant, type) ->
          getJdbcTemplateByTenant(tenant)
              .query(
                  """
                      select attributes
                      from salesforce_attributes
                      where lower(entity) = (?)
                      """,
                  rs ->
                      rs.next()
                          ? Arrays.stream(rs.getString(1).split(","))
                              .map(String::trim)
                              .collect(Collectors.toList())
                          : List.of(),
                  type.toLowerCase());
  public static Function<LocalDateTime, String> formatDate =
      (lastItemModifiedTime) ->
          Constants.getDateTimeString(
              salesforceDateTimeFormatter2,
              lastItemModifiedTime.atZone(TimeZone.getTimeZone(UTC).toZoneId()));
  public static ListData idsForSalesforceAccountFromDB =
      (tenant, arg) ->
          getJdbcTemplateByTenant(tenant)
              .query(
                  """
                      select distinct account_id
                      from salesforce_opportunity
                      where tt_end is null
                      """,
                  listDataExtractor);

  public static void createBulkJobsForGivenTimePeriod(
      String url,
      String type,
      String attributes,
      AuthUtil authUtil,
      LocalDateTime start,
      LocalDateTime end,
      List<String> ids,
      Long mileStoneId)
      throws Exception {
    String startTimeFormattedString = formatDate.apply(start);
    String endTimeFormattedString = formatDate.apply(end);
    String requestJsonString =
        getRequestJsonStringByType(
            type, attributes, startTimeFormattedString, endTimeFormattedString, ids);
    createJobAdapter(type, url, requestJsonString, mileStoneId, authUtil);
  }

  public static void createJobAdapter(
      String type, String url, String requestJsonString, Long mileStoneId, AuthUtil authUtil)
      throws Exception {
    RequestBody requestBody =
        RequestBody.create(
            MediaType.parse(org.springframework.http.MediaType.APPLICATION_JSON_VALUE),
            requestJsonString);
    Request request = UtilFunctions.createPostRequest(url, authUtil.getAccessToken(), requestBody);
    JSONObject jsonObject =
        UtilFunctions.makeWebRequest(
            requestJsonString,
            request,
            authUtil.getTenant(),
            authUtil,
            true,
            RequestType.Salesforce_JOB_CREATOR,
            type,
            platform,
            null);
    log.info("job creation response {}", jsonObject.toMap());
    UtilFunctions.getJdbcTemplateByTenant(authUtil.getTenant())
        .update(
            """
                insert into salesforce_jobs (id , entity, milestone_id, info, status, end_time, create_time, query)
                values (?::text, ?::text, ?::int4, ?::json, ?::text, ?::timestamp, ?::timestamp, ?::text);
                """,
            jsonObject.getString("id"),
            type,
            mileStoneId,
            jsonObject.toString(),
            Constants.created,
            null,
            jsonObject.getString("createdDate"),
            requestJsonString);
  }

  private static String getRequestJsonStringByType(
      String type,
      String attributes,
      String startTimeFormattedString,
      String endTimeFormattedString,
      List<String> ids) {
    if (!type.equalsIgnoreCase(SalesforceAccount)) {
      return """
          {
            "operation": "query",
            "query": "SELECT %s FROM %s where  LastModifiedDate >= %s and LastModifiedDate < %s ORDER BY LastModifiedDate asc",
            "contentType" : "CSV",
            "columnDelimiter" : "COMMA"
          }
          """
          .formatted(attributes, type, startTimeFormattedString, endTimeFormattedString);
    } else {
      return """
          {
            "operation": "query",
            "query": "SELECT %s FROM %s where Id in (%s) ORDER BY LastModifiedDate asc",
            "contentType" : "CSV",
            "columnDelimiter" : "COMMA"
          }
          """
          .formatted(
              attributes,
              type,
              Strings.join(
                  ids.stream()
                      .map(a -> '\'' + a + '\'')
                      .collect(Collectors.toList())
                      .listIterator(),
                  ','));
    }
  }

  // TODO check logic for account sync via LastModifiedDate
  public static void createNextJobsForSalesforce(SalesforceAuthUtil salesforceAuthUtil, String type)
      throws Exception {
    Map<String, Object> lastSyncDetails =
        getLastSyncTimeForSalesforce(type, salesforceAuthUtil.getTenant());
    Timestamp lastSync = (Timestamp) lastSyncDetails.get(syncBegin);
    Long mileStoneId = (Long) lastSyncDetails.get("id");
    LocalDateTime now = LocalDateTime.now(TimeZone.getTimeZone(UTC).toZoneId());
    LocalDateTime lastSyncTime =
        lastSync.toLocalDateTime().atZone(TimeZone.getTimeZone(UTC).toZoneId()).toLocalDateTime();
    if (type.equalsIgnoreCase(SalesforceAccount)) {
      LocalDateTime newLastSyncTime = lastSyncTime.plusDays(IncrementInDays);
      if (newLastSyncTime.isAfter(now)) {
        newLastSyncTime = now;
      }
      createJobs(salesforceAuthUtil, type, lastSyncTime, newLastSyncTime, mileStoneId);
    } else {
      do {
        LocalDateTime newLastSyncTime = lastSyncTime.plusDays(IncrementInDays);
        if (newLastSyncTime.isAfter(now)) {
          newLastSyncTime = now;
        }
        createJobs(salesforceAuthUtil, type, lastSyncTime, newLastSyncTime, mileStoneId);
        lastSyncTime = newLastSyncTime;
      } while (lastSyncTime.isBefore(now));
    }
  }

  private static Map<String, Object> getLastSyncTimeForSalesforce(String type, String tenant)
      throws ParseException {
    Map<String, Object> lastSyncDetails =
        new HashMap<>(
            UtilFunctions.getLastSyncAndMileStoneByDomainAndPlatform(type, tenant, platform, null));
    Timestamp lastSync = (Timestamp) lastSyncDetails.get(syncBegin);
    if (lastSync == null) {
      lastSync =
          Timestamp.from(
              new SimpleDateFormat("yyyy-MM-dd")
                  .parse("2017-01-01")
                  .toInstant()
                  .atZone(TimeZone.getTimeZone(UTC).toZoneId())
                  .toInstant());
    }
    lastSyncDetails.put(syncBegin, lastSync);
    return lastSyncDetails;
  }

  public static void createJobs(
      SalesforceAuthUtil salesforceAuthUtil,
      String type,
      LocalDateTime lastSyncTime,
      LocalDateTime newLastSyncTime,
      Long mileStoneId)
      throws Exception {
    String attributes = getAttributesByEntity(salesforceAuthUtil.getTenant(), type);
    if (type.equalsIgnoreCase(SalesforceAccount)) {
      createJobsForAccountEntity(
          salesforceAuthUtil, attributes, lastSyncTime, newLastSyncTime, mileStoneId);
    } else {
      createJobsForOtherEntities(
          salesforceAuthUtil, type, attributes, lastSyncTime, newLastSyncTime, mileStoneId);
    }
  }

  private static String getAttributesByEntity(String tenant, String type) {
    return Strings.join(attributesMap.get(tenant, type).listIterator(), ',');
  }

  private static void createJobsForOtherEntities(
      SalesforceAuthUtil salesforceAuthUtil,
      String type,
      String attributes,
      LocalDateTime lastSyncTime,
      LocalDateTime newLastSyncTime,
      Long mileStoneId)
      throws Exception {
    createBulkJobsForGivenTimePeriod(
        salesforceAuthUtil.getBulkJobQueryUrl(),
        type,
        attributes,
        salesforceAuthUtil,
        lastSyncTime,
        newLastSyncTime,
        null,
        mileStoneId);
  }

  private static void createJobsForAccountEntity(
      SalesforceAuthUtil salesforceAuthUtil,
      String attributes,
      LocalDateTime lastSyncTime,
      LocalDateTime newLastSyncTime,
      Long mileStoneId) {
    List<String> ids = getIdsForSalesforceAccount(salesforceAuthUtil.getTenant());
    if (ids == null || ids.size() == 0) return;
    List<List<String>> subSets = ListUtils.partition(ids, 1000);
    subSets.forEach(
        idlisT -> {
          try {
            createBulkJobsForGivenTimePeriod(
                salesforceAuthUtil.getBulkJobQueryUrl(),
                SalesforceAccount,
                attributes,
                salesforceAuthUtil,
                lastSyncTime,
                newLastSyncTime,
                idlisT,
                mileStoneId);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  private static List<String> getIdsForSalesforceAccount(String tenant) {
    return new HashSet<>(
            Objects.requireNonNull(idsForSalesforceAccountFromDB.get(tenant, null), tenant))
        .stream().toList();
  }

  public static List<Map<String, Object>> getJobsFromDBByType(String type, String tenant) {
    return UtilFunctions.getJdbcTemplateByTenant(tenant)
        .query(
            """
                select
                	row_to_json(rows) data
                from
                	(
                	select * From salesforce_jobs
                	where lower(entity) = lower(?)
                	and end_time is null
                	) rows
                """,
            Utils::extractListData,
            type);
  }

  public static void updateJobStatusInDB(
      SalesforceAuthUtil salesforceAuthUtil, String id, String status) {
    getJdbcTemplateByTenant(salesforceAuthUtil.getTenant())
        .update(
            """
                update salesforce_jobs set status = ?::text , end_time = ?::timestamp where id = ?::text
                """,
            status,
            Instant.now().toString(),
            id);
  }
}
