package com.pearrity.mantys.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZohoInvoice {
  private String invoice_id;

  private String customer_name;

  private Timestamp last_modified_time;

  private String customer_id;

  private String currency_code;

  private Double total;

  private String line_item_id;

  private String item_description;

  private String subject_content;

  //  private String json;

  private String company_name;

  protected Timestamp tt_begin;

  protected Timestamp tt_end;

  private String status;

  private Timestamp date;

  private String organization_id;

  private String recurring;
}
