package com.pearrity.mantys.quickBooks.reader.adapter;

import com.pearrity.mantys.hubspot.HubSpotResourceUtil;
import com.pearrity.mantys.interfaces.EtlReaderAdapter;
import com.pearrity.mantys.interfaces.RequestType;
import com.pearrity.mantys.quickBooks.QuickBooksAuthUtil;
import com.pearrity.mantys.utils.UtilFunctions;
import com.squareup.okhttp.Request;
import org.json.JSONObject;

import java.util.Objects;
import java.util.logging.Logger;

import static com.pearrity.mantys.quickBooks.QuickBooksResourceUtil.*;

public class ProfitAndLossReportReaderAdapter implements EtlReaderAdapter {

  private final Logger log = Logger.getLogger(ProfitAndLossReportReaderAdapter.class.getName());
  private final QuickBooksAuthUtil quickBooksAuthUtil;

  private final String type;

  private final String realmId;

  public ProfitAndLossReportReaderAdapter(
      QuickBooksAuthUtil quickBooksAuthUtil, String realmId, String type) {
    this.quickBooksAuthUtil = quickBooksAuthUtil;
    this.realmId = realmId;
    this.type = type;
  }

  @Override
  public JSONObject getResource(String val) {
    try {
      String startDay = val.split("--")[0];
      String endDay = val.split("--")[1];
      String summarizeByClasses =
          Objects.equals(type, ProfitAndLossReport)
              ? "&summarize_column_by=Classes&accounting_method=Accrual"
              : "";
      String url =
          String.format(
              quickBooksAuthUtil.resourceBaseUrl
                  + "/v3/company/%S/reports/"
                  + type
                  + "?start_date=%s&end_date=%s&minorversion=%d"
                  + summarizeByClasses,
              realmId,
              startDay,
              endDay,
              QuickBooksAuthUtil.minorVersion);
      log.info("url : " + url);
      Request request = UtilFunctions.createGetRequest(url, quickBooksAuthUtil.getAccessToken());
      return UtilFunctions.makeWebRequest(
          null,
          request,
          quickBooksAuthUtil.getTenant(),
          quickBooksAuthUtil,
          true,
          RequestType.Reader,
          type,
          platform,
          realmId);
    } catch (Exception e) {
      UtilFunctions.addToUnsuccessfulSync(
          val, quickBooksAuthUtil.getTenant(), type, HubSpotResourceUtil.platform, realmId);
      return new JSONObject();
    }
  }
}
