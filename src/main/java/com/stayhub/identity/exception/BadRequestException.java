package com.stayhub.identity.exception;

import java.util.Map;
import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {

  private final Map<String, String> errors;

  public BadRequestException(String message) {
    super(message);
    this.errors = null;
  }

  public BadRequestException(String message, Map<String, String> errors) {
    super(message);
    this.errors = errors;
  }
}
