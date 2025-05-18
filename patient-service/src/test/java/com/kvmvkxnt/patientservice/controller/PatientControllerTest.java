package com.kvmvkxnt.patientservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.kvmvkxnt.patientservice.dto.PatientRequestDTO;
import com.kvmvkxnt.patientservice.dto.PatientResponseDTO;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "classpath:data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PatientControllerTest {
  @Autowired TestRestTemplate restTemplate;

  @Test
  void shouldReturnListOfPatients() throws IOException {
    ResponseEntity<String> response = restTemplate.getForEntity("/patients", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  @DirtiesContext
  void shouldCreateANewPatient() {
    PatientRequestDTO newPatient = new PatientRequestDTO();
    newPatient.setName("John Doe");
    newPatient.setEmail("john@example.com");
    newPatient.setAddress("123 Main St");
    newPatient.setDateOfBirth("1990-01-01");
    newPatient.setRegisteredDate("2023-10-01");

    ResponseEntity<PatientResponseDTO> response =
        restTemplate.postForEntity("/patients", newPatient, PatientResponseDTO.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertTrue(response.getBody().getId() != null);
    assertEquals(newPatient.getName(), response.getBody().getName());
    assertEquals(newPatient.getEmail(), response.getBody().getEmail());
    assertEquals(newPatient.getAddress(), response.getBody().getAddress());
    assertEquals(newPatient.getDateOfBirth(), response.getBody().getDateOfBirth());
  }

  @Test
  void shouldNotCreateInvalidPatient() {
    PatientRequestDTO invalidPatient = new PatientRequestDTO();
    invalidPatient.setName("John Doe");
    invalidPatient.setEmail("invalid-email");
    invalidPatient.setAddress("123 Main St");
    invalidPatient.setDateOfBirth("1990-01-01");

    ResponseEntity<String> response =
        restTemplate.postForEntity("/patients", invalidPatient, String.class);

    DocumentContext documentContext = JsonPath.parse(response.getBody());

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("Registered date is mandatory", documentContext.read("$.registeredDate"));
    assertEquals("Email should be valid", documentContext.read("$.email"));
  }

  @Test
  void shouldNotCreatePatientWithExistingEmail() {
    PatientRequestDTO newPatient = new PatientRequestDTO();
    newPatient.setName("Jane Doe");
    newPatient.setEmail("john.doe@example.com");
    newPatient.setAddress("456 Elm St");
    newPatient.setDateOfBirth("1990-01-01");
    newPatient.setRegisteredDate("2023-10-01");

    ResponseEntity<String> response =
        restTemplate.postForEntity("/patients", newPatient, String.class);

    DocumentContext documentContext = JsonPath.parse(response.getBody());

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("Email already exists", documentContext.read("$.message"));
  }

  @Test
  @DirtiesContext
  void shouldUpdateAnExistingPatient() {
    PatientRequestDTO updatedPatient = new PatientRequestDTO();
    updatedPatient.setName("John Smith");
    updatedPatient.setEmail("john.doe@example.com");
    updatedPatient.setAddress("789 Oak St");
    updatedPatient.setDateOfBirth("1990-01-01");

    HttpEntity<PatientRequestDTO> requestEntity = new HttpEntity<>(updatedPatient);

    ResponseEntity<PatientResponseDTO> response =
        restTemplate.exchange(
            "/patients/123e4567-e89b-12d3-a456-426614174000",
            HttpMethod.PUT,
            requestEntity,
            PatientResponseDTO.class);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  void shouldNotUpdateNonExistentPatient() {
    PatientRequestDTO updatedPatient = new PatientRequestDTO();
    updatedPatient.setName("John Smith");
    updatedPatient.setEmail("john.doe@example.com");
    updatedPatient.setAddress("789 Oak St");
    updatedPatient.setDateOfBirth("1990-01-01");

    HttpEntity<PatientRequestDTO> requestEntity = new HttpEntity<>(updatedPatient);

    ResponseEntity<PatientResponseDTO> response =
        restTemplate.exchange(
            "/patients/5e6f1b1d-494e-4726-b44f-5194bbe9b4ee",
            HttpMethod.PUT,
            requestEntity,
            PatientResponseDTO.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void shouldNotAcceptInvalidUUID() {
    PatientRequestDTO updatedPatient = new PatientRequestDTO();
    updatedPatient.setName("John Smith");
    updatedPatient.setEmail("john.doe@example.com");
    updatedPatient.setAddress("789 Oak St");
    updatedPatient.setDateOfBirth("1990-01-01");

    HttpEntity<PatientRequestDTO> requestEntity = new HttpEntity<>(updatedPatient);

    ResponseEntity<PatientResponseDTO> response =
        restTemplate.exchange(
            "/patients/someshit", HttpMethod.PUT, requestEntity, PatientResponseDTO.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  @DirtiesContext
  void shouldDeleteAnExistingPatient() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/patients/123e4567-e89b-12d3-a456-426614174000",
            HttpMethod.DELETE,
            null,
            String.class);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  void shouldNotDeleteNonExistentPatient() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/patients/5e6f1b1d-494e-4726-b44f-5194bbe9b4ee",
            HttpMethod.DELETE,
            null,
            String.class);

    DocumentContext documentContext = JsonPath.parse(response.getBody());

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals("Patient not found", documentContext.read("$.message"));
  }
}
