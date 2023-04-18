package com.pearrity.mantys.view;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.pearrity.mantys.DataSourceContextHolder;
import com.pearrity.mantys.auth.util.AuthUserUtil;
import com.pearrity.mantys.domain.enums.Action;
import com.pearrity.mantys.repository.ViewRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.pearrity.mantys.domain.utils.Constants.*;
import static com.pearrity.mantys.domain.utils.UtilFunctions.*;

@Service
@Slf4j
public class ViewServiceImpl implements ViewService {

  @Autowired private AuthUserUtil authUserUtil;
  @Autowired private ViewRepository viewRepository;

  @Autowired private Environment environment;

  @Override
  public List<Map<String, Object>> getViewObjectList() {
    return viewRepository.getViewObjectList(authUserUtil.getUserIdFromSecurityContext());
  }

  @Override
  public HashMap getViewConfigByIdV2(Long objectId, Map<String, Object> map)
      throws JsonProcessingException, InterruptedException {
    Assert.assertTrue(
        viewRepository.checkPrivilegeByActionAndObject(
            Action.READ, objectId, authUserUtil.getUserIdFromSecurityContext()));
    boolean demo =
        Objects.equals(
            environment.getProperty("spring.profiles.active"),
            environment.getProperty("mantys.demo.profile", Strings.EMPTY));
    boolean jsonRes =
        Arrays.stream(
                environment.getProperty("mantys.demo.map-result-query", Strings.EMPTY).split(","))
            .anyMatch(a -> a.equals(DataSourceContextHolder.getTenant()));
    if (jsonRes == Boolean.TRUE) demo = false;
    boolean filter = map.get("industry") != null;
    return setDataForEachComponentV2(
        viewRepository.getViewConfigByObjectIdV2(objectId, demo, filter), map, demo);
  }

  @Override
  @Transactional
  public String saveViewLayout(Long objectId, Map<String, Object> map) {
    int count = 0;
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      count += viewRepository.saveViewLayout(objectId, entry);
    }
    return count + " rows updated";
  }

  // TODO rewrite
  @Override
  public Object updateInputField(Map<String, Object> map, Long objectId)
      throws JsonProcessingException, ParseException, InterruptedException {
    JSONObject jsonObject;
    if (!(map.containsKey("vt_begin") && map.containsKey("vt_end"))) throw new RuntimeException();
    else {
      jsonObject = new JSONObject(map);
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
      Date end =
          Date.from(
              dateFormat
                  .parse(jsonObject.getString("vt_end"))
                  .toInstant()
                  .atZone(ZoneId.of(UTC))
                  .toInstant());
      Date start =
          Date.from(
              dateFormat
                  .parse(jsonObject.getString("vt_begin"))
                  .toInstant()
                  .atZone(ZoneId.of(UTC))
                  .toInstant());
      Date startRefer = new Date(start.getTime());
      Date endRefer = new Date(end.getTime());

      List<Object> inputs = jsonObject.getJSONArray("inputs").toList();
      List<Integer> k =
          inputs.stream().map(a -> (Integer) ((Map<?, ?>) a).get("input_id")).toList();

      List<Map<String, Object>> list =
          viewRepository.sortViewInputList(
              objectId, k); // input_field_id , increment_type , increment_check_query
      for (Map<String, Object> i : list) {
        JSONObject inputData =
            new JSONObject(
                new Gson()
                    .toJson(
                        inputs.stream()
                            .filter(
                                a ->
                                    Objects.equals(
                                        ((Map<?, ?>) a).get("input_id"), i.get("input_field_id")))
                            .findFirst()
                            .orElseThrow()));
        viewRepository.saveViewInput(
            (Integer) i.get("input_field_id"),
            inputData.get("data").toString(),
            jsonObject.getString("vt_begin"),
            jsonObject.getString("vt_end"),
            authUserUtil.getUserIdFromSecurityContext());
        List<Map<String, Object>> actions =
            viewRepository.getActionsFromViewInputs((Integer) i.get("input_field_id"));
        for (Map<String, Object> action : actions) {
          String query = (String) action.get("query");
          boolean recursive = (boolean) action.get("recursive");
          Date finalEnd = dateFormat.parse("20240102");
          boolean b =
              viewRepository.checkIncrement((String) i.get("increment_check_query"), start, end);
          if (b) {
            while (end.toInstant().isBefore(finalEnd.toInstant())) {
              viewRepository.performActionAtViewInput(
                  query,
                  inputData.get("data").toString(),
                  dateFormat.format(start),
                  dateFormat.format(end),
                  dateFormat.format(startRefer),
                  dateFormat.format(endRefer));
              start = incrementMonth(start);
              end = incrementMonth(end);
              if (!recursive) break;
            }
          }
        }
      }
    }
    return getViewConfigByIdV2(objectId, jsonObject.getJSONObject("filters").toMap());
  }

  @Override
  public ResponseEntity<Object> customDataByType(String type, Long id, String attr, String entity) {
    Map<String, Object> map = viewRepository.customDataByType(type, id, attr, entity);
    if (map == null) {
      map = new HashMap<>();
      map.put("statusCode", 404);
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(map);
    } else {
      map.put("statusCode", 200);
      return ResponseEntity.ok(map);
    }
  }

  private HashMap setDataForEachComponentV2(HashMap res, Map<String, Object> params, boolean demo)
      throws JsonProcessingException, InterruptedException {
    if (res.get("view_layout") == null) {
      return res;
    }
    params = fixNullFilters(res, params);

    // Component Data query execution
    JSONArray array = new JSONArray(new Gson().toJson(res.get("view_layout")));

    Boolean fetchFilters = (Boolean) params.get("fetchFilters");
    Map<String, Object> finalParams = params;
    // Component Data query execution
    if (fetchFilters == null || !fetchFilters) {
      ExecutorService executorService = Executors.newFixedThreadPool(4);
      array.forEach(
          ob -> {
            executorService.submit(
                () -> {
                  JSONObject object = (JSONObject) ob;
                  String query = object.getString("query");
                  object.put(
                      "component_data", viewRepository.getComponentData(query, finalParams, demo));
                  object.remove("query");
                });
          });
      executorService.shutdown();
      executorService.awaitTermination(3, TimeUnit.MINUTES);
    }
    res.put("view_layout", new ObjectMapper().readValue(array.toString(), List.class));

    // Filter Query execution
    JSONArray filters = new JSONArray(new Gson().toJson(res.get("filters")));
    ExecutorService executorService = Executors.newFixedThreadPool(4);

    filters.forEach(
        filter -> {
          executorService.submit(
              () -> {
                JSONObject filterJson = (JSONObject) filter;
                String query = filterJson.getString("query");
                filterJson.put("allowed_values", viewRepository.getFilterData(query, finalParams));
                filterJson.remove("query");
              });
        });
    executorService.shutdown();
    executorService.awaitTermination(3, TimeUnit.MINUTES);
    res.put("filters", new ObjectMapper().readValue(filters.toString(), List.class));

    return res;
  }

  private Map<String, Object> fixNullFilters(HashMap res, Map<String, Object> params) {
    Map<String, Object> newParams = new HashMap<>();
    if (res.get("filters") == null) return newParams;
    JSONArray array = new JSONArray(new Gson().toJson(res.get("filters")));
    for (Object ob : array) {
      JSONObject object = (JSONObject) ob;
      String key = object.getString("filter_name");
      if (!Objects.equals(key, "month")) {
        newParams.put(key, null);
      }
    }
    newParams.put("asof", null);
    newParams.putAll(params);
    if (newParams.get("endDate") == null) {
      newParams.put(
          "endDate", LocalDate.now(ZoneId.of("UTC")).with(TemporalAdjusters.firstDayOfNextMonth()));
    }
    return newParams;
  }
}
