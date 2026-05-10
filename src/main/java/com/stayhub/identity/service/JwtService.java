package com.stayhub.identity.service;

import org.springframework.security.core.Authentication;

public interface JwtService {
  String generateToken(Authentication auth);
}
