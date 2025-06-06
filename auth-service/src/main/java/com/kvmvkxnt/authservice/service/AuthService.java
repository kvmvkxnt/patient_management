package com.kvmvkxnt.authservice.service;

import com.kvmvkxnt.authservice.dto.LoginRequestDTO;
import com.kvmvkxnt.authservice.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private final UserService userService;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;

  public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
    this.userService = userService;
    this.passwordEncoder = passwordEncoder;
    this.jwtUtil = jwtUtil;
  }

  public Optional<String> authenticate(LoginRequestDTO loginRequestDTO) {
    Optional<String> token =
        userService
            .findByUsername(loginRequestDTO.getUsername())
            .filter(u -> passwordEncoder.matches(loginRequestDTO.getPassword(), u.getPassword()))
            .map(u -> jwtUtil.generateToken(u.getUsername(), u.getRoles()));

    return token;
  }

  public boolean validateToken(String token) {
    try {
      jwtUtil.validateToken(token);
      return true;
    } catch (JwtException e) {
      return false;
    }
  }
}
