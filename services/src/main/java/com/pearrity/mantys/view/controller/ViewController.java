package com.pearrity.mantys.view.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.pearrity.mantys.view.ViewService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.Map;

@RestController
@RequestMapping("/services/view")
@SecurityRequirement(name = "token")
public class ViewController {

  @Autowired private ViewService viewService;

  @GetMapping("/getList")
  public ResponseEntity<Object> getViewObjectList() {
    return ResponseEntity.ok(viewService.getViewObjectList());
  }

  @PostMapping("/{objectId}/getConfig")
  public ResponseEntity<Object> getViewConfigById(
      @PathVariable Long objectId, @RequestBody Map<String, Object> map)
      throws JsonProcessingException, InterruptedException {
    return ResponseEntity.ok(viewService.getViewConfigByIdV2(objectId, map));
  }

  @PostMapping("/{objectId}/saveLayout")
  public ResponseEntity<String> saveViewLayout(
      @PathVariable Long objectId, @RequestBody Map<String, Object> map) {
    return ResponseEntity.ok(viewService.saveViewLayout(objectId, map));
  }

  @PostMapping("/{objectId}/update/inputField")
  public ResponseEntity<Object> updateInputField(
      @RequestBody Map<String, Object> map, @PathVariable Long objectId)
      throws JsonProcessingException, ParseException, InterruptedException {
    return ResponseEntity.ok(viewService.updateInputField(map, objectId));
  }

  @GetMapping("/{id}/customData")
  public ResponseEntity<Object> customData(
      @RequestParam String type,
      @RequestParam String attr,
      @PathVariable Long id,
      @RequestParam String entity) {
    return viewService.customDataByType(type, id, attr, entity);
  }
}
