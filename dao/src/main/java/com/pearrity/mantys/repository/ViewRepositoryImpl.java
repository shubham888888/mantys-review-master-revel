package com.pearrity.mantys.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.pearrity.mantys.domain.enums.Action;
import com.pearrity.mantys.repository.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Transactional
@Slf4j
public class ViewRepositoryImpl implements ViewRepository {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Override
  public List<Map<String, Object>> getViewObjectList(Long userId) {
    return jdbcTemplate.query(
        """
            select
            	row_to_json(rows) as data
            from
            	(
            	select
            		v.id as view_id ,
            		v.type as
              view_type ,
            		v.name as view_name ,
            		v.config ,
            		p. object_type ,
            		p.action ,
            		p. object_id ,
            		p.id privilege_id ,
            		v.filters
            	from
            		view_details v
            	inner join
              privilege_details p on
            		p. object_id = v. object_id
            	where
            		p. user_id =
             ?
            	order by
            		v.name ) as rows
            """,
        Utils::extractListData,
        userId);
  }

  @Override
  public List<Map<String, Object>> getViewConfigByObjectId(Long objectId) {
    return jdbcTemplate.query(
        """
            select
            	row_to_json(rows) data
            from
            	(
            	select
            		V.ID,
            		V. object_id ,
            		V. type_id ,
            		V.config,
            		V.name,
            		V.type,
            		VL.ID as view_layout_id ,
            		VL. view_id ,
            		VL. component_id ,
            		VL.position,
            		C.config as component_config ,
            		c. query_id ,
            		c.type as component_type ,
            		c.query ,
            		c.name as component_name
            	from
            		view_details V
            	inner join view_layout VL on
            		VL. view_id = V.ID
            	inner join component_details C on
            		C.ID = VL. component_id
            	where
            		V. object_id = ?) rows
            """,
        Utils::extractListData,
        objectId);
  }

  @Override
  public HashMap getViewConfigByObjectIdV2(Long id, boolean demo, boolean filter) {
    return jdbcTemplate.query(
        """

                                                         select
                                                         	row_to_json(rows) as data
                                                         from
                                                         	(
                                                         	select
                                                         		vd1.* ,
                                                         		(
                                                         		select
                                                         			json_agg(data)
                                                         		from
                                                         			(
                                                         			select
                                                         				cd.id as component_id,
                                                         				cd.name as component_name,
                                                         				cd.config as component_config,
                                                         				cd.query,
                                                         				vl.position,
                                                         				cd.type as component_type
                                                         			from
                                                         				(select * from component_details(? , ?)) cd
                                                         			inner join view_layout vl on
                                                         				vl.component_id = cd.id
                                                         				and cd.view_id = vl.view_id
                                                         				and (vl.disabled is null or vl.disabled = false)
                                                         			inner join view_details vd on
                                                         				vd.id = vl.view_id
                                                         			where
                                                         				vd.object_id = vd1.object_id) as data) as view_layout
                                                         	from
                                                         		view_details vd1
                                                         	where
                                                         		vd1.object_id = ? ) as rows

            """,
        rs -> {
          try {
            return rs.next()
                ? new ObjectMapper().readValue(rs.getString("data"), HashMap.class)
                : null;
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        },
        demo,
        filter,
        id);
  }

  @Override
  public boolean checkPrivilegeByActionAndObject(Action action, Long objId, Long userId) {
    return Boolean.TRUE.equals(
        jdbcTemplate.queryForObject(
            """

                select
                count(*) > 0
                from
                privileges  pd
                where
                pd. user_id  = ?
                and pd. action  = ?
                and pd. object_id  = ?

                """,
            Boolean.class,
            userId,
            action.name(),
            objId));
  }

  @Override
  public Object getComponentData(String query, Map<String, Object> map1, boolean demo) {
    try {
      if (demo) {
        return new NamedParameterJdbcTemplate(jdbcTemplate)
            .query(
                query,
                map1,
                rs -> rs.next() ? new Gson().fromJson(rs.getString("data"), List.class) : null);
      } else {
        return new NamedParameterJdbcTemplate(jdbcTemplate)
            .query(query, map1, Utils::extractListData);
      }
    } catch (Exception e) {
      log.error("error occurred during component query execution ", e);
      throw e;
    }
  }

  @Override
  public Object getFilterData(String query, Map<String, Object> map) {
    try {
      return new NamedParameterJdbcTemplate(jdbcTemplate)
          .query(
              query,
              map,
              rs -> rs.next() ? new Gson().fromJson(rs.getString("data"), List.class) : null);
    } catch (Exception exception) {
      log.error("error occurred during filter query execution ", exception);
      throw exception;
    }
  }

  @Override
  public int saveViewLayout(Long objectId, Map.Entry<String, Object> entry) {
    int[] types = {Types.OTHER, Types.INTEGER, Types.INTEGER};
    Object[] objects = {new Gson().toJson(entry.getValue()), objectId, entry.getKey()};
    return jdbcTemplate.update(
        """
                update
                	view_layout
                set
                	position = ?
                where
                	view_id = (
                	select
            		v.id
            	from
            		view v
            	where
            		v.object_id = ?)
            	and component_id = ?
            """,
        objects,
        types);
  }

  @Override
  public List<Map<String, Object>> sortViewInputList(Long objectId, List<Integer> list) {
    return new NamedParameterJdbcTemplate(jdbcTemplate)
        .query(
            """
                select row_to_json(rows) as data from (
                select
                    input_field_id , increment_type , increment_check_query
                from
                    view_inputs vi
                    inner join input_fields i on vi.input_field_id = i.id
                where
                    object_id = :objectId
                    and input_field_id in (:list)
                    order by ordering) as rows;
                	""",
            Map.of("objectId", objectId, "list", list),
            Utils::extractListData);
  }

  @Override
  public void saveViewInput(
      Integer i, String data, String vtBegin, String vtEnd, Long userIdFromSecurityContext) {
    Date now = new Date();
    jdbcTemplate.update(
        """
            update input_data_fields_from_view
            set tt_end = ?::timestamp
            where input_field_id = ?::int4
            and vt_begin = ?::timestamp
            and vt_end = ?::timestamp
            and tt_End is null
            """,
        now,
        i,
        vtBegin,
        vtEnd);
    jdbcTemplate.update(
        """
            insert into input_data_fields_from_view (data , vt_begin ,vt_end , tt_begin , updated_by_user_id , input_field_id)
            values (?::json,?::timestamp,?::timestamp,?::timestamp,?::int4,?::int4)
            """,
        new Gson().toJson(Map.of("data", data)),
        vtBegin,
        vtEnd,
        now,
        userIdFromSecurityContext,
        i);
  }

  @Override
  public List<Map<String, Object>> getActionsFromViewInputs(Integer i) {
    return jdbcTemplate.query(
        """
            select
            	row_to_json(rows) as data
            from
            	(
            	select
            		query , recursive
            	from
            		action_at_input_data
            	where
            		input_field_id = ?::int4
            	order by
            		ordering
                ) rows;
            """,
        Utils::extractListData,
        i);
  }

  @Override
  public void performActionAtViewInput(
      String query, String data, String vtBegin, String vtEnd, String startRefer, String endRefer) {
    Map<String, Object> map =
        Map.of(
            "data",
            data,
            "vtBegin",
            vtBegin,
            "vtEnd",
            vtEnd,
            "startRefer",
            startRefer,
            "endRefer",
            endRefer);
    new NamedParameterJdbcTemplate(jdbcTemplate).update(query, map);
  }

  // TODO
  @Override
  public HashMap customDataByType(String type, Long id, String attr, String entity) {
    type = type.replaceAll(" +", "");
    if (!(type.matches("[a-zA-Z_]*") && attr.matches("[a-zA-Z_]*"))) throw new RuntimeException();
    try {
      if (type.equalsIgnoreCase("invoices")) {
        return jdbcTemplate.query(
            "select row_to_json (rows) data from (select file as data , content_type from invoices"
                + " where id = ?::text and tt_end is null and realm_id = get_realm_id(?::text) "
                + " limit 1) rows",
            rs -> rs.next() ? new Gson().fromJson(rs.getString(1), HashMap.class) : null,
            id,
            entity);
      } else {
        type = "%" + type + "%";
        return jdbcTemplate.query(
            "select row_to_json (rows) data from (select file as data , content_type from"
                + " attachables where type_id = ?::text and lower(type) like lower(?::text) and"
                + " tt_end is null and realm_id = get_realm_id(?::text) limit 1) rows",
            rs -> rs.next() ? new Gson().fromJson(rs.getString(1), HashMap.class) : null,
            id,
            type,
            entity);
      }
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public boolean checkIncrement(String incrementCheckQuery, Date start, Date end) {
    return Boolean.TRUE.equals(
        new NamedParameterJdbcTemplate(jdbcTemplate)
            .queryForObject(
                incrementCheckQuery, Map.of("startRefer", start, "endRefer", end), Boolean.class));
  }
}
