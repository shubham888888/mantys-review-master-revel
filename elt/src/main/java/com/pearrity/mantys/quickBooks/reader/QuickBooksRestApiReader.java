package com.pearrity.mantys.quickBooks.reader;

import com.pearrity.mantys.interfaces.EtlLoader;
import com.pearrity.mantys.interfaces.EtlReaderAdapter;
import com.pearrity.mantys.quickBooks.QuickBooksAuthUtil;
import com.pearrity.mantys.quickBooks.QuickBooksResourceUtil;
import com.pearrity.mantys.quickBooks.reader.adapter.ProfitAndLossReportReaderAdapter;
import com.pearrity.mantys.quickBooks.reader.adapter.QuickBooksEntityReaderAdapter;
import com.pearrity.mantys.quickBooks.reader.loader.AttachmentListLoader;
import com.pearrity.mantys.quickBooks.reader.loader.QBEntityLoader;
import com.pearrity.mantys.quickBooks.reader.loader.QBlCustomerLoader;
import org.json.JSONObject;
import org.springframework.batch.item.ItemReader;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Logger;

import static com.pearrity.mantys.utils.UtilFunctions.addAllPreviousUnsuccessfulSyncIds;
import static com.pearrity.mantys.utils.UtilFunctions.getLastSyncByDomainAndPlatform;

public class QuickBooksRestApiReader implements ItemReader<JSONObject> {
  private final Logger log = Logger.getLogger(QuickBooksRestApiReader.class.getName());

  private final List<String> list;
  private final QuickBooksAuthUtil quickBooksAuthUtil;

  private final EtlReaderAdapter quickBooksReaderAdapter;

  public QuickBooksRestApiReader(QuickBooksAuthUtil quickBooksAuthUtil, String type, String realmId)
      throws Exception {
    this.quickBooksAuthUtil = quickBooksAuthUtil;
    EtlLoader loader = getLoaderInstance(type, realmId);
    Timestamp getLastSyncTime =
        getLastSyncByDomainAndPlatform(
            type, quickBooksAuthUtil.getTenant(), QuickBooksResourceUtil.platform, realmId);
    Set<String> strings = new HashSet<>(loader.load(getLastSyncTime));
    strings.addAll(
        addAllPreviousUnsuccessfulSyncIds(
            quickBooksAuthUtil.getTenant(), type, QuickBooksResourceUtil.platform, realmId));
    this.list = new ArrayList<>();
    this.list.addAll(strings);
    this.quickBooksReaderAdapter = getReaderAdapter(type, realmId);
  }

  @Override
  public JSONObject read() throws IOException {
    synchronized (list) {
      if (!list.isEmpty()) {
        return quickBooksReaderAdapter.getResource(list.remove(0));
      }
      return null;
    }
  }

  private EtlReaderAdapter getReaderAdapter(String type, String realmId) {
    if (Objects.equals(type, QuickBooksResourceUtil.ProfitAndLossReport)
        || Objects.equals(type, QuickBooksResourceUtil.ProfitAndLossDetail)) {
      return new ProfitAndLossReportReaderAdapter(quickBooksAuthUtil, realmId, type);
    } else {
      return new QuickBooksEntityReaderAdapter(quickBooksAuthUtil, realmId, type);
    }
  }

  private EtlLoader getLoaderInstance(String type, String realmId) {
    if (Objects.equals(type, QuickBooksResourceUtil.ProfitAndLossReport)
        || Objects.equals(type, QuickBooksResourceUtil.ProfitAndLossDetail)) {
      return new QBlCustomerLoader(quickBooksAuthUtil, realmId);
    } else if (type.equals(QuickBooksResourceUtil.Attachable2)) {
      return new AttachmentListLoader(quickBooksAuthUtil, type, realmId);
    } else {
      return new QBEntityLoader(quickBooksAuthUtil, realmId, type);
    }
  }
}
