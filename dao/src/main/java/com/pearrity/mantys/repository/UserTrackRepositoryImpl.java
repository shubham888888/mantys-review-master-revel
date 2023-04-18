package com.pearrity.mantys.repository;

import com.pearrity.mantys.domain.UserExperienceTrack;
import com.pearrity.mantys.domain.UserLoginTrack;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;

@Repository
@AllArgsConstructor
public class UserTrackRepositoryImpl implements UserTrackRepository {

  private JdbcTemplate jdbcTemplate;

  @Override
  public int saveUserIpAtLogin(UserLoginTrack userLoginTrack) {
    return jdbcTemplate.update(
        """
                        insert into user_login_track(user_id , ip_addr , login_time)
                            values(? , ? , ?::timestamp )
                        """,
        userLoginTrack.getUserId(),
        userLoginTrack.getIpAddr(),
        userLoginTrack.getLoginTime());
  }

  @Override
  public int saveUserNavigationAndFilterPreference(UserExperienceTrack track) {
    return jdbcTemplate.update(
        """
                        INSERT INTO public.user_experience_track
                         (user_id, view_id, filter_config, navigated_from_id)
                         VALUES( ?, ?, ?::json, ?);

                        """,
        new Object[] {
          track.getUserId(), track.getViewId(), track.getFilterConfig(), track.getNavigatedFrom()
        },
        new int[] {Types.INTEGER, Types.INTEGER, Types.OTHER, Types.INTEGER});
  }
}
