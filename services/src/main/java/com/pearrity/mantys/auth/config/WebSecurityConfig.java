package com.pearrity.mantys.auth.config;

import com.auth0.jwt.algorithms.Algorithm;
import com.pearrity.mantys.auth.util.AuthConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.pearrity.mantys.auth.JwtAuthenticationEntryPoint;
import com.pearrity.mantys.auth.filter.JWTAuthorizationFilter;
import com.pearrity.mantys.domain.MantysSecretKeys;
import com.pearrity.mantys.repository.config.AwsSecretsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static com.pearrity.mantys.domain.utils.Constants.mantysSecretKeys;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, jsr250Enabled = true, prePostEnabled = true)
public class WebSecurityConfig {

  @Autowired private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

  @Autowired AwsSecretsService awsSecretsService;

  @Value("#{'${mantys.cors.allowedOrigin.patterns}'.split(',')}")
  private List<String> allowedOrigins;

  @Autowired @Lazy private JWTAuthorizationFilter jwtAuthorizationFilter;

  /**
   * Override exempt endpoints
   *
   * @param http
   * @throws Exception
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    List<String> patternsToBypass = new ArrayList<>(List.of("/services/auth/**"));
    patternsToBypass.addAll(List.of("/swagger-ui/**", "/v3/api-docs/**"));
    http.cors()
        .and()
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .csrf()
        .disable()
        .authorizeRequests()
        .antMatchers(patternsToBypass.toArray(new String[0]))
        .permitAll()
        .anyRequest()
        .authenticated()
        .and()
        .exceptionHandling()
        .authenticationEntryPoint(jwtAuthenticationEntryPoint) // required else in case of not valid
        // authentication object in security context returns only http code in response without any
        // specific message
        .and()
        .addFilterBefore(jwtAuthorizationFilter, BasicAuthenticationFilter.class);

    return http.build();
  }

  /**
   * Allow CORS
   *
   * @return CorsConfigurationSource bean
   */
  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(allowedOrigins);
    configuration.setAllowCredentials(true);
    configuration.setAllowedHeaders(
        Arrays.asList(
            HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
            HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
            HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
            HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
            HttpHeaders.ORIGIN,
            HttpHeaders.CACHE_CONTROL,
            HttpHeaders.CONTENT_TYPE,
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.USER_AGENT,
            HttpHeaders.REFERER,
            AuthConstants.TOKEN_IDENTIFIER));
    configuration.setAllowedMethods(List.of(HttpMethod.GET.name(), HttpMethod.POST.name()));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  /**
   * HmacSHA256 algorithm to encrypt our created jwt token with the secret key given
   *
   * @return Algorithm bean
   */
  @Bean
  public Algorithm createJWTAuthenticator() {
    MantysSecretKeys secretKeys =
        awsSecretsService.getSecret(mantysSecretKeys, MantysSecretKeys.class);
    if (secretKeys == null) throw new RuntimeException();
    return Algorithm.HMAC256(secretKeys.getSecretKey());
  }

  /**
   * encrypts all passwords stored in db for users with BCryptPasswordEncoder
   *
   * @return passwordEncoder bean
   */
  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
