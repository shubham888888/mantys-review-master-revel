package com.pearrity.mantys.utils;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class CustomExceptionHandler {

  @Value("${mantys.print.errorStackTrace:false}")
  private String printStackTrace;

  @Value("${mantys.tech.mail}")
  private String techMail;

  /**
   * throw errors intentionally for 400 code else exceptions are thrown automatically due to some
   * causes giving 500 status code
   *
   * @param e
   * @return ResponseEntity
   */
  @ExceptionHandler(Exception.class)
  private ResponseEntity<Object> handleException(Exception e) {
    return sendExceptionResponse(e);
  }

  private ResponseEntity<Object> sendExceptionResponse(Exception e) {
    if (printStackTrace.equals(Boolean.TRUE.toString())) {
      log.error("error occurred : ", e);
    }
    if (e.getCause() instanceof Error) return sendErrorResponse((Error) e.getCause());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .message(
                String.format(
                    "Error occurred while processing your request. Please reach out to %s for"
                        + " further assistance.",
                    techMail))
            .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
            .build();
    return new ResponseEntity<>(errorResponse, errorResponse.httpStatus);
  }

  private ResponseEntity<Object> sendErrorResponse(Error e) {
    ErrorResponse errorResponse =
        ErrorResponse.builder().message(e.getMessage()).httpStatus(HttpStatus.BAD_REQUEST).build();
    return new ResponseEntity<>(errorResponse, errorResponse.httpStatus);
  }

  @Builder
  public record ErrorResponse(String message, HttpStatus httpStatus) {}
}
