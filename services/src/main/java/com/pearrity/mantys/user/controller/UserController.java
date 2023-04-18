package com.pearrity.mantys.user.controller;

import com.pearrity.mantys.auth.AuthService;
import com.pearrity.mantys.user.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/services/user")
@SecurityRequirement(name = "token")
public class UserController {

  @Autowired UserService userService;

  @Autowired AuthService authService;

  @GetMapping("/getDetails")
  private ResponseEntity<Object> getUserDetails() {
    return ResponseEntity.ok(userService.getUserDetails());
  }

  @GetMapping("/privileges")
  private ResponseEntity<Object> getUserPrivileges() {
    return ResponseEntity.ok(userService.getUserPrivileges());
  }

  @PostMapping("/logout")
  public ResponseEntity<Object> logoutCurrentUser() {
    return authService.logoutCurrentUser();
  }
}
