package com.pearrity.mantys.repository;

import com.pearrity.mantys.domain.UserExperienceTrack;
import com.pearrity.mantys.domain.UserLoginTrack;

public interface UserTrackRepository {
  int saveUserIpAtLogin(UserLoginTrack userLoginTrack);

  int saveUserNavigationAndFilterPreference(UserExperienceTrack track);
}
