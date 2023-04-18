package com.pearrity.mantys.zoho.processor;

import com.pearrity.mantys.repository.config.DbConfiguration;
import com.pearrity.mantys.repository.config.SpringContext;
import com.pearrity.mantys.zoho.ZohoInvoiceResourceUtil;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.ZonedDateTime;

public class ZohoInvoiceProcessor<T> {

  private final Class<T> type;

  private final ItemProcessor<T, T> itemProcessor;

  private final JdbcTemplate jdbcTemplate;

  public ZohoInvoiceProcessor(Class<?> k, String domain) {
    this.type = (Class<T>) k;
    DbConfiguration dbConfiguration = SpringContext.getBean(DbConfiguration.class);
    this.jdbcTemplate = dbConfiguration.getJdbcTemplateByDomain(domain);
    this.itemProcessor =
        t -> {
          Timestamp now = Timestamp.from(ZonedDateTime.now().toInstant());
          String id =
              (String) getFieldValueByFieldName(t, ZohoInvoiceResourceUtil.idLookupMap.get(type));
          Timestamp lastModifiedTime =
              (Timestamp) getFieldValueByFieldName(t, "last_modified_time");
          String organizationId = (String) getFieldValueByFieldName(t, "organization_id");
          String lineItemId = (String) getFieldValueByFieldName(t, "line_item_id");
          String query =
              String.format(
                  "update %s set tt_end = ? where %s = ? and last_modified_time < ? and"
                      + " organization_id = ? and tt_end is null and line_item_id = ?",
                  ZohoInvoiceResourceUtil.getEndPoint(t.getClass()),
                  ZohoInvoiceResourceUtil.idLookupMap.get(t.getClass()));
          int res =
              jdbcTemplate.update(query, now, id, lastModifiedTime, organizationId, lineItemId);
          if (res == 0) {
            Boolean exists =
                jdbcTemplate.queryForObject(
                    String.format(
                        "select count(*) > 0 from %s where %s = ? and  organization_id = ? and"
                            + " line_item_id = ? and tt_end is null",
                        ZohoInvoiceResourceUtil.getEndPoint(t.getClass()),
                        ZohoInvoiceResourceUtil.idLookupMap.get(t.getClass())),
                    Boolean.class,
                    id,
                    organizationId,
                    lineItemId);
            if (Boolean.TRUE.equals(exists)) {
              return null;
            } else {
              setField("tt_begin", t, now);
              return t;
            }
          } else {
            setField("tt_begin", t, now);
            return t;
          }
        };
  }

  private void setField(String fieldName, T t, Object value)
      throws NoSuchFieldException, IllegalAccessException {
    Field field = t.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(t, value);
  }

  private Object getFieldValueByFieldName(T t, String fieldName)
      throws NoSuchFieldException, IllegalAccessException {
    Field field = t.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    return field.get(t);
  }

  public ItemProcessor<T, T> getItemProcessor() {
    return itemProcessor;
  }
}
