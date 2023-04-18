package com.pearrity.mantys.repository.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {

  public static ResultSetExtractor<List<String>> listDataExtractor =
      rs -> {
        List<String> ids = new ArrayList<>();
        while (rs.next()) {
          ids.add(rs.getString(1));
        }
        return ids;
      };

  public static List<Map<String, Object>> extractListData(ResultSet rs) throws SQLException {
    List<Map<String, Object>> list = new ArrayList<>();
    while (rs.next()) {
      HashMap response;
      try {
        response = new ObjectMapper().readValue(rs.getString("data"), HashMap.class);
      } catch (JsonProcessingException | SQLException e) {
        throw new RuntimeException(e);
      }
      list.add(response);
    }
    return list;
  }
}
