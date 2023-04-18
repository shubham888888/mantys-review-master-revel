package com.pearrity.mantys.user.controller;

import com.pearrity.mantys.domain.UserExperienceTrack;
import com.pearrity.mantys.domain.UserLoginTrack;
import com.pearrity.mantys.user.UserTrackService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/services/track")
@SecurityRequirement(name = "token")
public class UserTrackingController {

  private UserTrackService userTrackService;

  @PostMapping("/userIp")
  public ResponseEntity<Object> saveUserIpAtLogin(@RequestBody UserLoginTrack userLoginTrack) {
    return userTrackService.saveUserIpAtLogin(userLoginTrack);
  }

  @PostMapping("/user/experience")
  public ResponseEntity<Object> saveUserNavigationAndFilterPreference(
      @RequestBody UserExperienceTrack track) {
    return userTrackService.saveUserNavigationAndFilterPreference(track);
  }
}
