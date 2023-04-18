package com.pearrity.mantys.metadata.controller;

import com.pearrity.mantys.domain.metadata.Currency;
import com.pearrity.mantys.metadata.service.MetadataService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.sql.Timestamp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/services/metadata")
@SecurityRequirement(name = "token")
@Slf4j
public class MetadataController {

  @Autowired MetadataService metadataService;

  @GetMapping("/getCurrencyRates")
  public ResponseEntity<Object> getCurrencyRates(
      @RequestParam(required = false, value = "baseCurrency") String baseCurrency,
      @RequestParam Timestamp validFrom,
      @RequestParam Timestamp validTo)
      throws Exception {
    return ResponseEntity.ok(metadataService.getCurrencyRates(baseCurrency, validFrom, validTo));
  }

  @PostMapping("/setCurrencyRates")
  public ResponseEntity<Object> setCurrencyRates(@RequestBody Currency currency) throws Exception {
    metadataService.setCurrencyRates(currency);
    return ResponseEntity.ok("Currency rates updated successfully");
  }
}
