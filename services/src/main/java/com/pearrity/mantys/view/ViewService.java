package com.pearrity.mantys.view;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

public interface ViewService {
  List<Map<String, Object>> getViewObjectList();

  Map<String, Object> getViewConfigByIdV2(Long objectId, Map<String, Object> map)
      throws JsonProcessingException, InterruptedException;

  String saveViewLayout(Long objectId, Map<String, Object> map);

  Object updateInputField(Map<String, Object> map, Long objectId)
      throws JsonProcessingException, ParseException, InterruptedException;

  ResponseEntity<Object> customDataByType(String type, Long id, String attr, String entity);
}
