package com.pearrity.mantys.user;

import com.pearrity.mantys.domain.UserExperienceTrack;
import com.pearrity.mantys.domain.UserLoginTrack;
import org.springframework.http.ResponseEntity;

public interface UserTrackService {
  ResponseEntity<Object> saveUserIpAtLogin(UserLoginTrack userLoginTrack);

  ResponseEntity<Object> saveUserNavigationAndFilterPreference(UserExperienceTrack track);
}
