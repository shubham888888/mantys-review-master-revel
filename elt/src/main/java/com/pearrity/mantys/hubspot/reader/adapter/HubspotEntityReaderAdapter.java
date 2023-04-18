package com.pearrity.mantys.hubspot.reader.adapter;

import com.pearrity.mantys.hubspot.HubSpotAuthUtil;
import com.pearrity.mantys.hubspot.HubSpotResourceUtil;
import com.pearrity.mantys.interfaces.EtlReaderAdapter;
import com.pearrity.mantys.interfaces.RequestType;
import com.pearrity.mantys.utils.UtilFunctions;
import com.squareup.okhttp.Request;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.pearrity.mantys.hubspot.HubSpotResourceUtil.*;

public class HubspotEntityReaderAdapter implements EtlReaderAdapter {

  private final HubSpotAuthUtil hubSpotAuthUtil;
  private final Logger log = Logger.getLogger(HubspotEntityReaderAdapter.class.getName());

  private final String type;

  public HubspotEntityReaderAdapter(HubSpotAuthUtil hubSpotAuthUtil, String type) {
    this.hubSpotAuthUtil = hubSpotAuthUtil;
    this.type = type;
  }

  @Override
  public JSONObject getResource(String id) throws IOException {
    try {
      String params = id;
      if (type.equalsIgnoreCase(Deals)) {
        String dealProperties =
            """
                account_status,amount_in_home_currency,api_consumption_current_week,api_consumption_last_day,api_consumption_last_month,api_consumption_month_to_date,assigned_sales_engineer,associated_campaign,associatedcompanyid,bdr_sdr,cb_currencycode,cb_dueinvoicescount,cb_duesince,cb_nextbillingamount,cb_nextbillingat,cb_planquantity,cb_ponumber,cb_product,cb_remainingbillingcycles,cb_setupfee,cb_subscriptionid,cb_subscriptionmrr,cb_subscriptionstatus,cb_totaldues,channel,chargebee_subscription_site,closed_lost__dormant__dropped_reason__details_,closed_lost__dormant_reason,company_revenue_range,contracted_value,country,days_to_close,deal_category,deal_currency_code,deal_lifecycle_stage,deal_tier_type,gamekeeper_account_created,hs_acv,hs_all_assigned_business_unit_ids,hs_all_collaborator_owner_ids,hs_all_deal_split_owner_ids,hs_analytics_latest_source,hs_analytics_latest_source_company,hs_analytics_latest_source_contact,hs_analytics_latest_source_data_1,hs_analytics_latest_source_data_1_company,hs_analytics_latest_source_data_1_contact,hs_analytics_latest_source_data_2,hs_analytics_latest_source_data_2_company,hs_analytics_latest_source_data_2_contact,hs_analytics_latest_source_timestamp,hs_analytics_latest_source_timestamp_company,hs_analytics_latest_source_timestamp_contact,hs_analytics_source,hs_analytics_source_data_1,hs_analytics_source_data_2,hs_arr,hs_campaign,hs_closed_amount,hs_closed_amount_in_home_currency,hs_created_by_user_id,hs_date_entered_13507827,hs_date_entered_13594622,hs_date_entered_13594623,hs_date_entered_16172369,hs_date_entered_16172370,hs_date_entered_16172371,hs_date_entered_16172372,hs_date_entered_16172373,hs_date_entered_16172374,hs_date_entered_18286393,hs_date_entered_18286394,hs_date_entered_18286395,hs_date_entered_18286396,hs_date_entered_18286397,hs_date_entered_18286398,hs_date_entered_18286399,hs_date_entered_23381129,hs_date_entered_23381130,hs_date_entered_23381131,hs_date_entered_23381132,hs_date_entered_23381134,hs_date_entered_23381135,hs_date_entered_27717984,hs_date_entered_33938220,hs_date_entered_34132609,hs_date_entered_34132610,hs_date_entered_34132611,hs_date_entered_34132612,hs_date_entered_34132613,hs_date_entered_34132614,hs_date_entered_34132615,hs_date_entered_34138434,hs_date_entered_34154890,hs_date_entered_35431460,hs_date_entered_36154794,hs_date_entered_36154795,hs_date_entered_36731921,hs_date_entered_44362818,hs_date_entered_54569287,hs_date_entered_54569288,hs_date_entered_54569289,hs_date_entered_54569290,hs_date_entered_54569291,hs_date_entered_54569292,hs_date_entered_54569293,hs_date_entered_57468056,hs_date_entered_57468057,hs_date_entered_57468058,hs_date_entered_57468059,hs_date_entered_57468060,hs_date_entered_57468061,hs_date_entered_appointmentscheduled,hs_date_entered_closedlost,hs_date_entered_closedwon,hs_date_entered_contractsent,hs_date_entered_decisionmakerboughtin,hs_date_entered_presentationscheduled,hs_date_entered_qualifiedtobuy,hs_date_exited_13507827,hs_date_exited_13594622,hs_date_exited_13594623,hs_date_exited_16172369,hs_date_exited_16172370,hs_date_exited_16172371,hs_date_exited_16172372,hs_date_exited_16172373,hs_date_exited_16172374,hs_date_exited_18286393,hs_date_exited_18286394,hs_date_exited_18286395,hs_date_exited_18286396,hs_date_exited_18286397,hs_date_exited_18286398,hs_date_exited_18286399,hs_date_exited_23381129,hs_date_exited_23381130,hs_date_exited_23381131,hs_date_exited_23381132,hs_date_exited_23381134,hs_date_exited_23381135,hs_date_exited_27717984,hs_date_exited_33938220,hs_date_exited_34132609,hs_date_exited_34132610,hs_date_exited_34132611,hs_date_exited_34132612,hs_date_exited_34132613,hs_date_exited_34132614,hs_date_exited_34132615,hs_date_exited_34138434,hs_date_exited_34154890,hs_date_exited_35431460,hs_date_exited_36154794,hs_date_exited_36154795,hs_date_exited_36731921,hs_date_exited_44362818,hs_date_exited_54569287,hs_date_exited_54569288,hs_date_exited_54569289,hs_date_exited_54569290,hs_date_exited_54569291,hs_date_exited_54569292,hs_date_exited_54569293,hs_date_exited_57468056,hs_date_exited_57468057,hs_date_exited_57468058,hs_date_exited_57468059,hs_date_exited_57468060,hs_date_exited_57468061,hs_date_exited_appointmentscheduled,hs_date_exited_closedlost,hs_date_exited_closedwon,hs_date_exited_contractsent,hs_date_exited_decisionmakerboughtin,hs_date_exited_presentationscheduled,hs_date_exited_qualifiedtobuy,hs_deal_amount_calculation_preference,hs_deal_stage_probability,hs_deal_stage_probability_shadow,hs_exchange_rate,hs_forecast_amount,hs_forecast_probability,hs_is_closed,hs_is_closed_won,hs_lastmodifieddate,hs_likelihood_to_close,hs_line_item_global_term_hs_discount_percentage,hs_line_item_global_term_hs_discount_percentage_enabled,hs_line_item_global_term_hs_recurring_billing_period,hs_line_item_global_term_hs_recurring_billing_period_enabled,hs_line_item_global_term_hs_recurring_billing_start_date,hs_line_item_global_term_hs_recurring_billing_start_date_enabled,hs_line_item_global_term_recurringbillingfrequency,hs_line_item_global_term_recurringbillingfrequency_enabled,hs_manual_forecast_category,hs_merged_object_ids,hs_mrr,hs_next_step,hs_num_target_accounts,hs_object_id,hs_pinned_engagement_id,hs_predicted_amount,hs_predicted_amount_in_home_currency,hs_priority,hs_projected_amount,hs_projected_amount_in_home_currency,hs_read_only,hs_tag_ids,hs_tcv,hs_time_in_13507827,hs_time_in_13594622,hs_time_in_13594623,hs_time_in_16172369,hs_time_in_16172370,hs_time_in_16172371,hs_time_in_16172372,hs_time_in_16172373,hs_time_in_16172374,hs_time_in_18286393,hs_time_in_18286394,hs_time_in_18286395,hs_time_in_18286396,hs_time_in_18286397,hs_time_in_18286398,hs_time_in_18286399,hs_time_in_23381129,hs_time_in_23381130,hs_time_in_23381131,hs_time_in_23381132,hs_time_in_23381134,hs_time_in_23381135,hs_time_in_27717984,hs_time_in_33938220,hs_time_in_34132609,hs_time_in_34132610,hs_time_in_34132611,hs_time_in_34132612,hs_time_in_34132613,hs_time_in_34132614,hs_time_in_34132615,hs_time_in_34138434,hs_time_in_34154890,hs_time_in_35431460,hs_time_in_36154794,hs_time_in_36154795,hs_time_in_36731921,hs_time_in_44362818,hs_time_in_54569287,hs_time_in_54569288,hs_time_in_54569289,hs_time_in_54569290,hs_time_in_54569291,hs_time_in_54569292,hs_time_in_54569293,hs_time_in_57468056,hs_time_in_57468057,hs_time_in_57468058,hs_time_in_57468059,hs_time_in_57468060,hs_time_in_57468061,hs_time_in_appointmentscheduled,hs_time_in_closedlost,hs_time_in_closedwon,hs_time_in_contractsent,hs_time_in_decisionmakerboughtin,hs_time_in_presentationscheduled,hs_time_in_qualifiedtobuy,hs_unique_creation_key,hs_updated_by_user_id,hs_user_ids_of_all_notification_followers,hs_user_ids_of_all_notification_unfollowers,hs_user_ids_of_all_owners,hubspot_owner_assigneddate,hubspotcontact,industry__as_per_linkedin_,is_this_deal_a_joint_effort_with_a_partner_,key_status,last_usage_month,main_verticals,marketing_source,marketing_source_details,marketing_status_flag___deal,partner_name,pricing_model,primary_product__service,product___service,region,specialised_vertical,sub_vertical,vertical,dealname,amount,dealstage,pipeline,closedate,createdate,engagements_last_meeting_booked,engagements_last_meeting_booked_campaign,engagements_last_meeting_booked_medium,engagements_last_meeting_booked_source,hs_latest_meeting_activity,hs_sales_email_last_replied,hubspot_owner_id,notes_last_contacted,notes_last_updated,notes_next_activity_date,num_contacted_notes,num_notes,hs_createdate,hubspot_team_id,dealtype,hs_all_owner_ids,description,hs_all_team_ids,hs_all_accessible_team_ids,num_associated_contacts,closed_lost_reason,closed_won_reason
                 """;
        params = params + "?properties=" + dealProperties;
      } else if (type.equalsIgnoreCase(Companies)) {
        String companyProperties =
            """
                about_us,cb_subscription_start,cb_totaldueinvoicescount,cb_totaldues,cb_totalinvoiceamountpaid,cb_totalnoofsubscription,cb_totalsubscriptionmrr,closedate_timestamp_earliest_value_a2a17e6e,company_account_stage,company_revenue_range,company_size,customer_sentiment,earliest_log_date,facebookfans,first_contact_createdate_timestamp_earliest_value_78b50eea,first_conversion_date,first_conversion_date_timestamp_earliest_value_61f58f2c,first_conversion_event_name,first_conversion_event_name_timestamp_earliest_value_68ddae0a,first_deal_created_date,founded_year,g2_buyer_intent_activity_level,g2_buyer_intent_buying_stage,g2_buyer_intent_details,g2_buyer_intent_stack_last_identified,g2_buyer_intent_stack_products,gamekeeper_account_name,hs_additional_domains,hs_all_assigned_business_unit_ids,hs_analytics_first_timestamp,hs_analytics_first_timestamp_timestamp_earliest_value_11e3a63a,hs_analytics_first_touch_converting_campaign,hs_analytics_first_touch_converting_campaign_timestamp_earliest_value_4757fe10,hs_analytics_first_visit_timestamp,hs_analytics_first_visit_timestamp_timestamp_earliest_value_accc17ae,hs_analytics_last_timestamp,hs_analytics_last_timestamp_timestamp_latest_value_4e16365a,hs_analytics_last_touch_converting_campaign,hs_analytics_last_touch_converting_campaign_timestamp_latest_value_81a64e30,hs_analytics_last_visit_timestamp,hs_analytics_last_visit_timestamp_timestamp_latest_value_999a0fce,hs_analytics_latest_source,hs_analytics_latest_source_data_1,hs_analytics_latest_source_data_2,hs_analytics_latest_source_timestamp,hs_analytics_num_page_views,hs_analytics_num_page_views_cardinality_sum_e46e85b0,hs_analytics_num_visits,hs_analytics_num_visits_cardinality_sum_53d952a6,hs_analytics_source,hs_analytics_source_data_1,hs_analytics_source_data_1_timestamp_earliest_value_9b2f1fa1,hs_analytics_source_data_2,hs_analytics_source_data_2_timestamp_earliest_value_9b2f9400,hs_analytics_source_timestamp_earliest_value_25a3a52c,hs_avatar_filemanager_key,hs_created_by_user_id,hs_createdate,hs_date_entered_52149046,hs_date_entered_customer,hs_date_entered_evangelist,hs_date_entered_lead,hs_date_entered_marketingqualifiedlead,hs_date_entered_opportunity,hs_date_entered_other,hs_date_entered_salesqualifiedlead,hs_date_entered_subscriber,hs_date_exited_52149046,hs_date_exited_customer,hs_date_exited_evangelist,hs_date_exited_lead,hs_date_exited_marketingqualifiedlead,hs_date_exited_opportunity,hs_date_exited_other,hs_date_exited_salesqualifiedlead,hs_date_exited_subscriber,hs_ideal_customer_profile,hs_is_target_account,hs_last_booked_meeting_date,hs_last_logged_call_date,hs_last_open_task_date,hs_last_sales_activity_date,hs_last_sales_activity_timestamp,hs_last_sales_activity_type,hs_lastmodifieddate,hs_latest_createdate_of_active_subscriptions,hs_merged_object_ids,hs_num_blockers,hs_num_contacts_with_buying_roles,hs_num_decision_makers,hs_num_open_deals,hs_object_id,hs_pinned_engagement_id,hs_pipeline,hs_predictivecontactscore_v2_next_max_max_d4e58c1e,hs_read_only,hs_target_account,hs_target_account_probability,hs_target_account_recommendation_snooze_time,hs_target_account_recommendation_state,hs_time_in_52149046,hs_time_in_customer,hs_time_in_evangelist,hs_time_in_lead,hs_time_in_marketingqualifiedlead,hs_time_in_opportunity,hs_time_in_other,hs_time_in_salesqualifiedlead,hs_time_in_subscriber,hs_total_deal_value,hs_unique_creation_key,hs_updated_by_user_id,hs_user_ids_of_all_notification_followers,hs_user_ids_of_all_notification_unfollowers,hs_user_ids_of_all_owners,hubspot_owner_assigneddate,is_public,last_week_console_logins,lead_tier_type,linkedin_industry,num_associated_contacts,num_associated_deals,num_conversion_events,num_conversion_events_cardinality_sum_d095f14b,primary_industry,recent_conversion_date,recent_conversion_date_timestamp_latest_value_72856da1,recent_conversion_event_name,recent_conversion_event_name_timestamp_latest_value_66c820bf,recent_deal_amount,recent_deal_close_date,region,timezone,total_console_logins,total_money_raised,total_revenue,totango_status,name,twitterhandle,phone,twitterbio,twitterfollowers,address,address2,facebook_company_page,city,linkedin_company_page,linkedinbio,state,googleplus_page,engagements_last_meeting_booked,engagements_last_meeting_booked_campaign,engagements_last_meeting_booked_medium,engagements_last_meeting_booked_source,hs_latest_meeting_activity,hs_sales_email_last_replied,hubspot_owner_id,notes_last_contacted,notes_last_updated,notes_next_activity_date,num_contacted_notes,num_notes,zip,country,hubspot_team_id,hs_all_owner_ids,website,domain,hs_all_team_ids,hs_all_accessible_team_ids,numberofemployees,industry,annualrevenue,lifecyclestage,hs_lead_status,hs_parent_company_id,type,description,hs_num_child_companies,hubspotscore,createdate,closedate,first_contact_createdate,days_to_close,web_technologies
                 """;
        params = params + "?properties=" + companyProperties;
      }
      return checkDealsCompanyId(
          new JSONObject(executeEntityRequest(hubSpotAuthUtil, type, params, true)), id);
    } catch (Exception e) {
      UtilFunctions.addToUnsuccessfulSync(
          id, hubSpotAuthUtil.getTenant(), type, HubSpotResourceUtil.platform, null);
      return new JSONObject();
    }
  }

  private JSONObject checkDealsCompanyId(final JSONObject jsonObject, final String id)
      throws Exception {
    JSONObject newResponse = null;
    if (Objects.equals(type, Deals)) {
      if (!jsonObject.has("associatedcompanyid")
          || Objects.equals(jsonObject.optString("associatedcompanyid", "null"), "null")) {
        String url =
            hubSpotAuthUtil.resourceBaseUrl(Deals) + "/%s/associations/companies".formatted(id);
        log.info("associations/companies request : " + url);
        Request request = UtilFunctions.createGetRequest(url, hubSpotAuthUtil.getAccessToken());
        JSONObject associationJson =
            UtilFunctions.makeWebRequest(
                null,
                request,
                hubSpotAuthUtil.getTenant(),
                hubSpotAuthUtil,
                true,
                RequestType.Metadata,
                type,
                platform,
                null);

        try {
          newResponse = processDealCompanyAssociation(jsonObject, associationJson);
        } catch (Exception e) {
          log.log(Level.SEVERE, e.getMessage());
        }
      }
    }
    return newResponse == null ? jsonObject : newResponse;
  }

  private JSONObject processDealCompanyAssociation(
      JSONObject jsonObject, JSONObject associationJson) {
    for (Object ob : associationJson.getJSONArray("results")) {
      try {
        JSONObject item = (JSONObject) ob;
        boolean isPrimary =
            item.has("associationTypes")
                && item.getJSONArray("associationTypes").toList().stream()
                    .anyMatch(
                        a -> {
                          String label = (String) ((HashMap<?, ?>) a).get("label");
                          return (label != null && label.equalsIgnoreCase("Primary"));
                        });
        if (!isPrimary
            && item.has("type")
            && Objects.equals(item.getString("type"), "deal_to_company")) {
          jsonObject.put("associatedcompanyid", String.valueOf(item.get("id")));
          break;
        }
        if (isPrimary) {
          jsonObject.put("associatedcompanyid", String.valueOf(item.get("toObjectId")));
          break;
        }
      } catch (Exception e) {
        log.info(e.getMessage());
      }
    }
    return jsonObject;
  }
}
