package com.stayhub.identity.service;

import com.stayhub.identity.enums.Role;

public interface JwtService {
  String generateToken(String email, Role role);
}
