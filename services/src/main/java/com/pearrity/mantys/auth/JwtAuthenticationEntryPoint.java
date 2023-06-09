package com.pearrity.mantys.auth;

import com.pearrity.mantys.auth.util.AuthConstants;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@ControllerAdvice
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException, ServletException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    Exception exception = (Exception) request.getAttribute(AuthConstants.EXCEPTION);

    String message;
    if (exception != null) {
      message = exception.toString();
    } else if (authException.getCause() != null) {
      message = authException.getCause().toString() + " " + authException.getMessage();
    } else {
      message = "Unauthorized";
    }
    JSONObject object = new JSONObject();
    object.put("message", message);
    response.getWriter().write(object.toString());
  }
}
