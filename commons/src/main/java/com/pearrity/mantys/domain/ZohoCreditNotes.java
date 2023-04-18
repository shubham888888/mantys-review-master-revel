package com.pearrity.mantys.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ZohoCreditNotes {

  private String creditnote_id;

  private Timestamp last_modified_time;

  private String credit_note_status;

  private String currency_code;

  private String customer_name;

  private String customer_id;

  private String company_name;

  private Double discount_amount;

  private String recurring;

  private Double total;

  private String line_item_id;

  private String item_description;

  //  private String json;

  protected Timestamp tt_begin;

  protected Timestamp tt_end;

  private Timestamp date;

  private String organization_id;
}
