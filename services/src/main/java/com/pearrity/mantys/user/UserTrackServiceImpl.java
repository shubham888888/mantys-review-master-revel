package com.pearrity.mantys.user;

import com.pearrity.mantys.auth.util.AuthUserUtil;
import com.pearrity.mantys.domain.UserExperienceTrack;
import com.pearrity.mantys.domain.UserLoginTrack;
import com.pearrity.mantys.repository.UserTrackRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

@Service
@AllArgsConstructor
public class UserTrackServiceImpl implements UserTrackService {

  private UserTrackRepository userTrackRepository;

  private AuthUserUtil authUserUtil;

  @Override
  public ResponseEntity<Object> saveUserIpAtLogin(UserLoginTrack userLoginTrack) {
    if (userLoginTrack.getLoginTime() == null)
      userLoginTrack.setLoginTime(Timestamp.from(Instant.now()));
    if (Objects.equals(authUserUtil.getUserIdFromSecurityContext(), userLoginTrack.getUserId())) {
      userTrackRepository.saveUserIpAtLogin(userLoginTrack);
      return ResponseEntity.ok("successfully saved login information ...");
    } else return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
  }

  @Override
  public ResponseEntity<Object> saveUserNavigationAndFilterPreference(UserExperienceTrack track) {
    track.setUserId(authUserUtil.getUserIdFromSecurityContext());
    userTrackRepository.saveUserNavigationAndFilterPreference(track);
    return ResponseEntity.ok("saved user navigation and filter preference ...");
  }
}
