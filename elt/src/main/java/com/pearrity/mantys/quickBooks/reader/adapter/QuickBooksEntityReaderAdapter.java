package com.pearrity.mantys.quickBooks.reader.adapter;

import com.pearrity.mantys.hubspot.HubSpotResourceUtil;
import com.pearrity.mantys.interfaces.EtlReaderAdapter;
import com.pearrity.mantys.interfaces.RequestType;
import com.pearrity.mantys.quickBooks.QuickBooksAuthUtil;
import com.pearrity.mantys.quickBooks.QuickBooksResourceUtil;
import com.pearrity.mantys.utils.UtilFunctions;
import com.squareup.okhttp.Request;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;

import static com.pearrity.mantys.quickBooks.QuickBooksResourceUtil.*;

public class QuickBooksEntityReaderAdapter implements EtlReaderAdapter {

  private final QuickBooksAuthUtil quickBooksAuthUtil;

  private final String type;

  private final String realmId;

  public QuickBooksEntityReaderAdapter(
      QuickBooksAuthUtil quickBooksAuthUtil, String realmId, String type) {
    this.quickBooksAuthUtil = quickBooksAuthUtil;
    this.realmId = realmId;
    this.type =
        type.contains(QuickBooksResourceUtil.Attachable) ? QuickBooksResourceUtil.Attachable : type;
  }

  @Override
  public JSONObject getResource(String val) {
    try {
      String body = String.format("select * from %s where id = '%s'", type, val);
      JSONArray array =
          (JSONArray) executeQuery(body, quickBooksAuthUtil, realmId, type).get("array");
      JSONObject jsonObject = array.getJSONObject(0);
      if (Objects.equals(type, QuickBooksResourceUtil.Invoice)) {
        jsonObject.put("pdf", getInvoicePdf(val));
      }
      if (Objects.equals(type, Attachable)) {
        jsonObject.put("file", getAttachableFile(jsonObject.getString("TempDownloadUri")));
      }
      return jsonObject;
    } catch (Exception e) {
      UtilFunctions.addToUnsuccessfulSync(
          val, quickBooksAuthUtil.getTenant(), type, HubSpotResourceUtil.platform, realmId);
      return new JSONObject();
    }
  }

  private String getAttachableFile(String val) throws Exception {
    Request request = new Request.Builder().url(val).method("GET", null).build();
    return UtilFunctions.makeWebRequest(
            null,
            request,
            quickBooksAuthUtil.getTenant(),
            quickBooksAuthUtil,
            false,
            RequestType.File,
            Attachable,
            platform,
            realmId)
        .getString("data");
  }

  private String getInvoicePdf(String val) throws Exception {
    Request request =
        new Request.Builder()
            .url(
                quickBooksAuthUtil.resourceBaseUrl
                    + "/v3/company/"
                    + realmId
                    + "/invoice/"
                    + val
                    + "/pdf?minorversion="
                    + QuickBooksAuthUtil.minorVersion)
            .method("GET", null)
            .addHeader("Accept", "application/pdf")
            .addHeader("Authorization", quickBooksAuthUtil.getAccessToken())
            .addHeader("Content-Type", "application/pdf")
            .build();
    return UtilFunctions.makeWebRequest(
            null,
            request,
            quickBooksAuthUtil.getTenant(),
            quickBooksAuthUtil,
            false,
            RequestType.File,
            Invoice,
            platform,
            realmId)
        .getString("data");
  }
}
