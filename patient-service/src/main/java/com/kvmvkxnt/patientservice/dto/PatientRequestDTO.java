package com.kvmvkxnt.patientservice.dto;

import com.kvmvkxnt.patientservice.validators.CreatePatientValidationGroup;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PatientRequestDTO {

  @NotBlank(message = "Name is mandatory")
  @Size(max = 100, message = "Name cannot exceed 100 characters")
  private String name;

  @NotBlank(message = "Email is mandatory")
  @Email(message = "Email should be valid")
  private String email;

  @NotBlank(message = "Username is mandatory")
  private String username;

  @NotBlank(message = "Address is mandatory")
  private String address;

  @NotBlank(message = "Date of Birth is mandatory")
  private String dateOfBirth;

  @NotBlank(groups = CreatePatientValidationGroup.class, message = "Registered date is mandatory")
  private String registeredDate;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getDateOfBirth() {
    return dateOfBirth;
  }

  public void setDateOfBirth(String dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  public String getRegisteredDate() {
    return registeredDate;
  }

  public void setRegisteredDate(String registeredDate) {
    this.registeredDate = registeredDate;
  }
}
