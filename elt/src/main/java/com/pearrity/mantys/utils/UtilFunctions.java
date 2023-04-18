package com.pearrity.mantys.utils;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.pearrity.mantys.EncryptionService;
import com.pearrity.mantys.domain.EtlMetadata;
import com.pearrity.mantys.interfaces.AuthUtil;
import com.pearrity.mantys.interfaces.RequestType;
import com.pearrity.mantys.repository.config.DbConfiguration;
import com.pearrity.mantys.repository.config.SpringContext;
import com.squareup.okhttp.*;
import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotNull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.pearrity.mantys.domain.utils.Constants.*;

public class UtilFunctions {
  public static final EncryptionService encryptionService =
      SpringContext.getBean(EncryptionService.class);
  private static final Logger log = Logger.getLogger(UtilFunctions.class.getName());

  public static Object makeWebRequest(HttpRequest request, String key, Integer retry)
      throws IOException, InterruptedException {
    HttpClient client = HttpClient.newBuilder().build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    log.log(Level.FINE, "request {}", request.uri());
    if (response.statusCode() != 200) throw new RuntimeException("error processing request");
    if (key != null) {
      JSONObject object = new JSONObject(response.body());
      try {
        return object.getString(key);
      } catch (Exception e) {
        log.info(response.body());
        if (retry < 3 && key.equalsIgnoreCase("access_token")) {
          retry++;
          return makeWebRequest(request, key, retry);
        } else {
          throw e;
        }
      }
    } else {
      return response;
    }
  }

  public static String prepareQuery(Class<?> type, String tableName) {
    Field[] superclassFields;
    List<Field> fieldList = new ArrayList<>();
    if (type.getSuperclass() != null) {
      superclassFields = type.getSuperclass().getDeclaredFields();
      fieldList.addAll(Arrays.asList(superclassFields));
    }
    Field[] selfFields = type.getDeclaredFields();
    fieldList.addAll(Arrays.asList(selfFields));
    String fields =
        StringUtils.collectionToCommaDelimitedString(
            fieldList.stream().map(Field::getName).toList());
    StringBuilder stringBuilder = new StringBuilder();
    fieldList.forEach(a -> stringBuilder.append(" :").append(a.getName()).append(","));
    String placeHolders = stringBuilder.substring(0, stringBuilder.length() - 1);
    return String.format("insert into %s (%s) values (%s)", tableName, fields, placeHolders);
  }

  public static JdbcTemplate getJdbcTemplateByTenant(String tenant) {
    return SpringContext.getBean(DbConfiguration.class).getJdbcTemplateByDomain(tenant);
  }

  public static EtlMetadata getEtlMetadataByDomainAndPlatform(
      String type, String tenant, String platform) {
    return getJdbcTemplateByTenant(tenant)
        .query(
            "select * from etl_metadata where lower(domain) = lower(?) and lower(platform) ="
                + " lower(?) limit 1",
            rs -> rs.next() ? extractSingleEtlMetadata(rs) : null,
            type,
            platform);
  }

  public static EtlMetadata extractSingleEtlMetadata(ResultSet rs) throws SQLException {
    return EtlMetadata.builder()
        .id(rs.getLong("id"))
        .insertQuery(rs.getString("insert_query"))
        .jsonKeys(rs.getString("json_keys"))
        .platform(rs.getString("platform"))
        .sqlTypes(rs.getString("sql_types"))
        .domain(rs.getString("domain"))
        .build();
  }

  public static String getAuthHeader(String clientId, String clientSecret) {
    byte[] bytesEncoded = Base64.encodeBase64((clientId + ":" + clientSecret).getBytes());
    String base64ClientIdSec = new String(bytesEncoded);
    return "Basic " + base64ClientIdSec;
  }

  public static JSONObject makeWebRequest(
      String requestBody,
      Request request,
      String tenant,
      AuthUtil authUtil,
      boolean jsonData,
      RequestType requestType,
      String entity,
      String platform,
      String realmId)
      throws Exception {
    Response response = executeRequest(request);
    String bodyRes;
    JSONObject responseJson;
    try (ResponseBody body = response.body()) {
      if (jsonData) {
        bodyRes = body.string();
        if (!bodyRes.isBlank()) {
          try {
            responseJson =
                new JSONObject(bodyRes)
                    .put(responseHeadersFromResponse, response.headers().toMultimap())
                    .put(responseCodeFromResponse, response.code());
          } catch (Exception e) {
            log.log(
                Level.SEVERE,
                "Error occurred while making web request: "
                    + request.toString()
                    + " "
                    + e.getMessage());
            responseJson =
                new JSONObject()
                    .put(responseHeadersFromResponse, response.headers().toMultimap())
                    .put(responseCodeFromResponse, response.code());
          }
        } else {
          return new JSONObject()
              .put(responseHeadersFromResponse, response.headers().toMultimap())
              .put(responseCodeFromResponse, response.code());
        }
      } else {
        bodyRes = java.util.Base64.getEncoder().encodeToString(body.byteStream().readAllBytes());
        responseJson =
            new JSONObject()
                .put("data", bodyRes)
                .put(responseHeadersFromResponse, response.headers().toMultimap())
                .put(responseCodeFromResponse, response.code());
      }
    }
    saveRequestIntoDB(
        request.urlString(),
        tenant,
        jsonData,
        requestType,
        entity,
        platform,
        request.method(),
        requestBody,
        realmId,
        response.code(),
        bodyRes);
    if (isResponseStatus401AndAccessTokenReset(authUtil, response))
      return chainedWebRequest(
          requestBody,
          request,
          tenant,
          authUtil,
          jsonData,
          requestType,
          entity,
          platform,
          realmId,
          0);
    return responseJson;
  }

  public static JSONObject chainedWebRequest(
      String requestBody,
      Request request,
      String tenant,
      AuthUtil authUtil,
      boolean jsonData,
      RequestType requestType,
      String entity,
      String platform,
      String realmId,
      int retry)
      throws Exception {
    if (retry >= 3) return null;
    Response response = executeRequest(request);
    String bodyRes;
    JSONObject responseJson;
    try (ResponseBody body = response.body()) {
      if (jsonData) {
        bodyRes = body.string();
        try {
          responseJson =
              new JSONObject(bodyRes)
                  .put(responseHeadersFromResponse, response.headers().toMultimap())
                  .put(responseCodeFromResponse, response.code());
        } catch (Exception e) {
          responseJson = new JSONObject(bodyRes);
        }
      } else {
        bodyRes = java.util.Base64.getEncoder().encodeToString(body.byteStream().readAllBytes());
        responseJson =
            new JSONObject()
                .put("data", bodyRes)
                .put(responseHeadersFromResponse, response.headers().toMultimap())
                .put(responseCodeFromResponse, response.code());
      }
    }
    saveRequestIntoDB(
        request.urlString(),
        tenant,
        jsonData,
        requestType,
        entity,
        platform,
        request.method(),
        requestBody,
        realmId,
        response.code(),
        bodyRes);
    if (isResponseStatus401AndAccessTokenReset(authUtil, response))
      return chainedWebRequest(
          requestBody,
          request,
          tenant,
          authUtil,
          jsonData,
          requestType,
          entity,
          platform,
          realmId,
          ++retry);
    return responseJson;
  }

  private static Response executeRequest(Request request) throws IOException {
    return new OkHttpClient().newCall(request).execute();
  }

  private static void saveRequestIntoDB(
      String urlString,
      String tenant,
      boolean jsonData,
      RequestType requestType,
      String entity,
      String platform,
      String method,
      String body,
      String realmId,
      int code,
      String responseBody) {
    log.info("saving web request into db");
    getJdbcTemplateByTenant(tenant)
        .update(
            """
                insert into etl_web_requests(url , json_Data , request_type , entity , platform , method , body , realm_id ,code ,response_body)
                values ( ?::text ,?::boolean ,?::text ,?::text ,?::text ,?::text ,?::text ,?::text ,?::int4 ,?::text )
                """,
            urlString,
            jsonData,
            requestType.name(),
            entity,
            platform,
            method,
            body,
            realmId,
            code,
            responseBody);
  }

  public static Request createPostRequest(String url, String accessToken, RequestBody body) {
    log.info("creating post request with url : " + url);
    return new Request.Builder()
        .url(url)
        .method("POST", body)
        .addHeader("Accept", "application/json")
        .addHeader("Content-Type", body.contentType().type())
        .addHeader("Authorization", accessToken)
        .build();
  }

  public static Object getObjectOrDefault(JSONObject jsonObject, String key, Object defaultValue) {
    try {
      return jsonObject.get(key);
    } catch (Exception e) {
      return defaultValue;
    }
  }

  public static Request createGetRequest(String url, String accessToken) {
    return createGetRequestWithAcceptHeader(url, accessToken, MediaType.APPLICATION_JSON_VALUE);
  }

  public static Request createGetRequestWithAcceptHeader(
      String url, String accessToken, String acceptHeader) {
    log.info("creating get request with url : " + url);
    return new Request.Builder()
        .url(url)
        .method("GET", null)
        .addHeader("Accept", acceptHeader)
        .addHeader("Authorization", accessToken)
        .build();
  }

  public static boolean isResponseStatus401AndAccessTokenReset(AuthUtil authUtil, Response response)
      throws Exception {
    if (response.code() == 200) return false;
    else if (response.code() == 401) {
      authUtil.setAccessToken();
      return true;
    }
    log.log(Level.SEVERE, "status code : " + response.code());
    log.log(Level.SEVERE, "request code : " + response.request().toString());
    log.log(Level.SEVERE, "response code : " + response.body().string());
    throw new RuntimeException("");
  }

  public static Timestamp getLastSyncByDomainAndPlatform(
      String type, String tenant, String platform, String org) {
    JdbcTemplate jdbcTemplate =
        SpringContext.getBean(DbConfiguration.class).getJdbcTemplateByDomain(tenant);
    return jdbcTemplate.query(
        """
            select
                sync_begin
            from
                etl_milestones
            where
                domain = ?
                and lower(platform) = ?
                and sync_end is null
                and (?::text is null or org = ?::text)
            """,
        rs -> rs.next() ? rs.getTimestamp("sync_begin") : null,
        type,
        platform,
        org,
        org);
  }

  public static Map<String, Object> getLastSyncAndMileStoneByDomainAndPlatform(
      String type, String tenant, String platform, String org) {
    JdbcTemplate jdbcTemplate =
        SpringContext.getBean(DbConfiguration.class).getJdbcTemplateByDomain(tenant);
    return jdbcTemplate.query(
        """
            select
                sync_begin,id
            from
                etl_milestones
            where
                domain = ?
                and lower(platform) = ?
                and sync_end is null
                and (?::text is null or org = ?::text)
            """,
        rs ->
            rs.next()
                ? Map.of(syncBegin, rs.getTimestamp(syncBegin), "id", rs.getLong("id"))
                : Map.of(),
        type,
        platform,
        org,
        org);
  }

  public static void updateLastSyncByDomainAndPlatform(
      String latestDateCovered, String tenant, String domain, String platform, String org) {
    JdbcTemplate jdbcTemplate =
        SpringContext.getBean(DbConfiguration.class).getJdbcTemplateByDomain(tenant);
    jdbcTemplate.update(
        """
            update
                etl_milestones
            set
                sync_end = ?::timestamp
            where
                domain = ?
                and platform = ?
                and sync_end is null
                and (?::text is null or org = ?::text)
            """,
        latestDateCovered,
        domain,
        platform,
        org,
        org);
    jdbcTemplate.update(
        """
            insert
                into
                etl_milestones(
                sync_begin ,
                domain ,
                platform, org)
            values (
              ?::timestamp,
              ?,
              ?, ?)
            """,
        latestDateCovered,
        domain,
        platform,
        org);
  }

  public static Collection<String> addAllPreviousUnsuccessfulSyncIds(
      String tenant, String domain, String platform, String org) {
    platform = platform.toLowerCase();
    domain = domain.toLowerCase();
    JdbcTemplate jdbcTemplate = UtilFunctions.getJdbcTemplateByTenant(tenant);
    List<String> strings =
        (List<String>)
            jdbcTemplate.query(
                """
                    select id
                    from etl_unsuccessful_sync
                    where lower(domain) = ?
                      and lower(platform) = ?
                      and (?::text is null or org_id = ?::text)
                      """,
                (ResultSetExtractor<Collection<String>>)
                    rs -> {
                      List<String> list = new ArrayList<>();
                      while (rs.next()) {
                        list.add(rs.getString("id"));
                      }
                      return list;
                    },
                domain,
                platform,
                org,
                org);
    if (strings != null && strings.size() > 0) {
      HashMap<String, Object> map = new HashMap<>();
      map.put("strings", strings);
      map.put("domain", domain);
      map.put("platform", platform);
      map.put("org", org);
      new NamedParameterJdbcTemplate(jdbcTemplate)
          .update(
              """
                     delete
                     from etl_unsuccessful_sync
                     where id in (:strings)
                       and lower(domain) =
                           :domain
                       and lower(platform) = :platform
                       and (:org::text is null or org_id = :org::text)
                  """,
              map);
    }
    return strings;
  }

  public static void updateRefreshTokenIfPresent(
      String tenant, String refreshToken, String realmId, String platform) {
    try {
      JdbcTemplate jdbcTemplate = getJdbcTemplateByTenant(tenant);
      refreshToken = encryptionService.encrypt(refreshToken);
      jdbcTemplate.update(
          """
                update etl_creds
                set tt_end = now()::timestamp
                where (?::text is null
                   or realm_id = ?::text) and tt_end is null and platform = ?::text
              """,
          realmId,
          realmId,
          platform);
      jdbcTemplate.update(
          """
              insert
                  into
                  public.etl_creds (
                  realm_id,
                  refresh_token,
                  tt_begin,
                  platform)
              values(?::text,
                 ?::text,
                 now()::timestamp,
                 ?::text);
              """,
          realmId,
          refreshToken,
          platform);
    } catch (Exception e) {
      log.log(Level.SEVERE, e.getMessage());
      throw new RuntimeException();
    }
  }

  public static String getRefreshTokenFromRealmIdAndPlatform(
      String tenant, String realmId, String platform) {
    try {
      return encryptionService.decrypt(
          Objects.requireNonNull(
                  getJdbcTemplateByTenant(tenant)
                      .queryForObject(
                          """
                                 select refresh_token
                                  from etl_creds
                                  where tt_end is null and platform = ?::text
                                  and (?::text is null
                                     or realm_id = ?::text)
                                  limit 1
                              """,
                          String.class,
                          platform,
                          realmId,
                          realmId))
              .trim());
    } catch (Exception e) {
      log.log(Level.SEVERE, e.getMessage());
      throw new RuntimeException();
    }
  }

  public static void addToUnsuccessfulSync(
      String id, String tenant, String type, String platform, String org) {
    getJdbcTemplateByTenant(tenant)
        .update(
            "insert into etl_unsuccessful_sync (domain , id , platform , org_id) values (? , ? , ?"
                + " , ?)",
            type,
            id,
            platform,
            org);
  }

  public static void updateEtlMileStone(
      StepExecution stepExecution,
      String tenant,
      String platform,
      String entity,
      Date from,
      String org) {
    if (stepExecution
        .getExitStatus()
        .getExitCode()
        .equalsIgnoreCase(ExitStatus.COMPLETED.getExitCode())) {
      updateLastSyncByDomainAndPlatform(from.toString(), tenant, entity, platform, org);
    }
  }

  public static JSONArray convertPlainCsvStringToJsonString(@NotNull String st) {
    JSONArray array = new JSONArray();
    try {
      CsvSchema csv = CsvSchema.emptySchema().withHeader();
      CsvMapper csvMapper = new CsvMapper();
      MappingIterator<Map<?, ?>> mappingIterator =
          csvMapper
              .reader()
              .forType(Map.class)
              .with(csv)
              .readValues(new ByteArrayInputStream(st.getBytes(StandardCharsets.UTF_8)));
      List<Map<?, ?>> list = mappingIterator.readAll();
      list.forEach(a -> array.put(new JSONObject(a)));
    } catch (Exception e) {
      e.printStackTrace();
    }
    return array;
  }

  public static Request createDeleteRequest(String url, String accessToken) {
    return new Request.Builder()
        .url(url)
        .method(HttpMethod.DELETE.name(), null)
        .addHeader("Authorization", accessToken)
        .build();
  }
}
