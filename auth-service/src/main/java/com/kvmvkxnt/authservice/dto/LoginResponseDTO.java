package com.kvmvkxnt.authservice.dto;

public class LoginResponseDTO {
  private final String token;

  public LoginResponseDTO(final String token) {
    this.token = token;
  }

  public String getToken() {
    return token;
  }
}
