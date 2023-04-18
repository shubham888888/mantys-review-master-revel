package com.pearrity.mantys.auth.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.pearrity.mantys.DataSourceContextHolder;
import com.pearrity.mantys.auth.util.AuthConstants;
import com.pearrity.mantys.EncryptionService;
import com.pearrity.mantys.domain.auth.AuthPrincipal;
import com.pearrity.mantys.domain.enums.Role;
import com.pearrity.mantys.domain.utils.UtilFunctions;
import com.pearrity.mantys.repository.UserRepository;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class JWTAuthorizationFilter extends OncePerRequestFilter {

  @Autowired private Algorithm algorithm;
  @Lazy @Autowired private UserRepository userRepository;

  @Autowired private EncryptionService encryptionService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain filterChain)
      throws ServletException, IOException {
    String token = req.getHeader("token");
    if (Objects.nonNull(token)) {
      try {
        token = encryptionService.decrypt(token);
      } catch (NoSuchPaddingException | IllegalBlockSizeException | InvalidKeyException |
               BadPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
        log.error("failed to decrypt token {}", token);
        token = req.getHeader("token");
      }
      UsernamePasswordAuthenticationToken authentication =
          verifyJWTAndGetAuthenticationToken(token, req, res);
      if (authentication != null)
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    filterChain.doFilter(req, res);
  }

  /**
   * verifies JwtToken And gets AuthenticationToken For saving into SecurityContext to make user
   * authenticated
   *
   * @param token jwtToken string
   * @param request HttpServletRequest
   * @param res HttpServletResponse
   * @return UsernamePasswordAuthenticationToken
   */
  private UsernamePasswordAuthenticationToken verifyJWTAndGetAuthenticationToken(
      String token, HttpServletRequest request, HttpServletResponse res) {

    try {
      DecodedJWT decodedJWT =
          JWT.require(algorithm).withIssuer(AuthConstants.MANTYS_DOMAIN).build().verify(token);
      String email = decodedJWT.getClaim(AuthConstants.EMAIL).asString();
      String role = decodedJWT.getClaim(AuthConstants.ROLE).asString();
      String name = decodedJWT.getClaim(AuthConstants.NAME).asString();
      Long userId = decodedJWT.getClaim(AuthConstants.USER_ID).asLong();
      Collection<SimpleGrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority(role));
      log.debug("{}", Arrays.toString(authorities.toArray()));
      DataSourceContextHolder.setTenant(UtilFunctions.getDomainFromEmail(email));
      AuthPrincipal principal = buildCurrentUserAuthPrincipal(userId, email, role, name);
      return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    } catch (Exception ex) {
      request.setAttribute(AuthConstants.EXCEPTION, ex);
    }
    return null;
  }

  private AuthPrincipal buildCurrentUserAuthPrincipal(
      Long userId, String email, String role, String name) {
    return AuthPrincipal.builder()
        .userId(userId)
        .email(email)
        .role(Role.valueOf(role))
        .domain(UtilFunctions.getDomainFromEmail(email))
        .name(name)
        .build();
  }
}
