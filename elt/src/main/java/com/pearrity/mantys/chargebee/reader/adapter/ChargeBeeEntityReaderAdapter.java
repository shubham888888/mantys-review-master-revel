package com.pearrity.mantys.chargebee.reader.adapter;

import com.pearrity.mantys.chargebee.ChargeBeeAuthUtil;
import com.pearrity.mantys.chargebee.ChargeBeeResourceUtil;
import com.pearrity.mantys.interfaces.EtlReaderAdapter;
import org.json.JSONObject;

import static com.pearrity.mantys.utils.UtilFunctions.addToUnsuccessfulSync;

public class ChargeBeeEntityReaderAdapter implements EtlReaderAdapter {

  private final ChargeBeeAuthUtil chargeBeeAuthUtil;

  private final String type;

  public ChargeBeeEntityReaderAdapter(ChargeBeeAuthUtil hubSpotAuthUtil, String type) {
    this.chargeBeeAuthUtil = hubSpotAuthUtil;
    this.type = type;
  }

  @Override
  public JSONObject getResource(String val) {
    try {
      String url = chargeBeeAuthUtil.resourceBaseUrl(type) + "/" + val;
      return ChargeBeeResourceUtil.executeSingleEntityRequest(chargeBeeAuthUtil, type, url);
    } catch (Exception e) {
      addToUnsuccessfulSync(
          val, chargeBeeAuthUtil.getTenant(), type, ChargeBeeResourceUtil.platform, null);
      return new JSONObject();
    }
  }
}
