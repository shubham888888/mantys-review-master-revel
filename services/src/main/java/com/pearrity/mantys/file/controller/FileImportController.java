package com.pearrity.mantys.file.controller;

import com.pearrity.mantys.domain.FileImport;
import com.pearrity.mantys.domain.Page;
import com.pearrity.mantys.file.FileImportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/services/files")
@SecurityRequirement(name = "token")
public class FileImportController {

  @Autowired private FileImportService fileImportsService;

  @PostMapping("/import")
  public ResponseEntity<Object> importFile(
      @RequestBody @Valid FileImport fileImport, @RequestParam(required = false) Boolean forced) {
    return fileImportsService.importFile(fileImport, forced);
  }

  @GetMapping(value = {"/templates"})
  public ResponseEntity<Object> getAllFileImportTemplates() {
    return fileImportsService.getFileImportTemplates();
  }

  @GetMapping(value = {"/imports"})
  public ResponseEntity<Object> getAllImports(@ModelAttribute Page page) {
    return fileImportsService.getAllImports(page);
  }

  @GetMapping(value = {"/imports/byId/{id}"})
  public ResponseEntity<Object> getImportById(@PathVariable Long id) {
    return fileImportsService.getImportById(id);
  }

  @GetMapping(value = {"/imports/{name}"})
  public ResponseEntity<Object> getAllImportsByName(@PathVariable String name) {
    return fileImportsService.getActiveImportByName(name);
  }

  @GetMapping("/imports/{name}/check")
  public ResponseEntity<Object> isFileNameImported(@PathVariable String name) {
    return fileImportsService.isFileNameImported(name);
  }
}
