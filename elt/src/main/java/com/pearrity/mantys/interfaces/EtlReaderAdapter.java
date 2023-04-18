package com.pearrity.mantys.interfaces;

import org.json.JSONObject;

import java.io.IOException;

public interface EtlReaderAdapter {
  JSONObject getResource(String val) throws IOException;
}
