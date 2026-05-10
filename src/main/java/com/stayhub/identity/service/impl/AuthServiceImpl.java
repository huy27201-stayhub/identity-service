package com.stayhub.identity.service.impl;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.stayhub.identity.dto.request.LoginRequest;
import com.stayhub.identity.dto.request.RegisterRequest;
import com.stayhub.identity.dto.response.LoginResponse;
import com.stayhub.identity.enums.Role;
import com.stayhub.identity.exception.BadRequestException;
import com.stayhub.identity.model.User;
import com.stayhub.identity.repository.UserRepository;
import com.stayhub.identity.service.AuthService;
import com.stayhub.identity.service.JwtService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
  private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;

  @Override
  public String register(RegisterRequest request) throws BadRequestException {
    validatePasswordConfirmation(request);

    String normalizedPhoneNumber =
        normalizePhoneNumber(request.getPhoneNumber(), request.getRegionCode());

    String normalizedEmail = request.getEmail().trim().toLowerCase();

    validateEmailAndPhoneUniqueness(normalizedEmail, normalizedPhoneNumber);

    User user =
        User.builder()
            .email(normalizedEmail)
            .password(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phoneNumber(normalizedPhoneNumber)
            .role(Role.ROLE_GUEST)
            .build();

    try {
      userRepository.save(user);
    } catch (DataIntegrityViolationException e) {
      validateEmailAndPhoneUniqueness(user.getEmail(), user.getPhoneNumber());
      throw e;
    }

    return "success";
  }

  @Override
  public LoginResponse login(LoginRequest request) throws BadRequestException {
    Authentication auth =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail().trim().toLowerCase(), request.getPassword()));

    String accessToken = jwtService.generateToken(auth);
    return LoginResponse.builder().accessToken(accessToken).build();
  }

  private void validateEmailAndPhoneUniqueness(String email, String phoneNumber) {
    Map<String, String> errors = new HashMap<>();

    if (userRepository.existsByEmail(email)) {
      errors.put("email", "Email is already taken");
    }

    if (userRepository.existsByPhoneNumber(phoneNumber)) {
      errors.put("phoneNumber", "Phone number is already taken");
    }

    if (!errors.isEmpty()) {
      throw new BadRequestException("Validation failed", errors);
    }
  }

  private void validatePasswordConfirmation(RegisterRequest request) throws BadRequestException {
    if (!request.getPassword().equals(request.getConfirmPassword())) {
      throw new BadRequestException(
          "Validation failed", Map.of("confirmPassword", "Password does not match"));
    }
  }

  private String normalizePhoneNumber(String rawPhoneNumber, String regionCode)
      throws BadRequestException {
    try {
      Phonenumber.PhoneNumber parsedPhoneNumber = phoneNumberUtil.parse(rawPhoneNumber, regionCode);

      if (!phoneNumberUtil.isValidNumber(parsedPhoneNumber)) {
        throw new BadRequestException(
            "Validation failed", Map.of("phoneNumber", "Invalid phone number"));
      }

      return phoneNumberUtil.format(parsedPhoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
    } catch (NumberParseException e) {
      throw new BadRequestException(
          "Validation failed", Map.of("phoneNumber", "Invalid phone number"));
    }
  }
}
