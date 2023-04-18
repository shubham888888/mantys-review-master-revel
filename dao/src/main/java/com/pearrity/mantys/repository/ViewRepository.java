package com.pearrity.mantys.repository;

import com.pearrity.mantys.domain.enums.Action;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ViewRepository {
  List<Map<String, Object>> getViewObjectList(Long userId);

  List<Map<String, Object>> getViewConfigByObjectId(Long id);

  HashMap getViewConfigByObjectIdV2(Long id, boolean demo, boolean filter);

  boolean checkPrivilegeByActionAndObject(Action read, Long id, Long userId);

  Object getComponentData(String query, Map<String, Object> map1, boolean demo);

  Object getFilterData(String query, Map<String, Object> map);

  int saveViewLayout(Long objectId, Map.Entry<String, Object> entry);

  List<Map<String, Object>> sortViewInputList(Long objectId, List<Integer> list);

  void saveViewInput(
      Integer i, String data, String vtBegin, String vtBegin1, Long userIdFromSecurityContext);

  List<Map<String, Object>> getActionsFromViewInputs(Integer i);

  void performActionAtViewInput(
      String query, String data, String vtBegin, String vtEnd, String startRefer, String endRefer);

  // TODO
  HashMap<String, Object> customDataByType(String type, Long id, String attr, String entity);

  boolean checkIncrement(String incrementCheckQuery, Date start, Date end);
}
