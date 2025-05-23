package com.kvmvkxnt.authservice.service;

import com.kvmvkxnt.authservice.model.User;
import com.kvmvkxnt.authservice.repository.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public Optional<User> findByUsername(String username) {
    return userRepository.findByUsername(username);
  }
}
