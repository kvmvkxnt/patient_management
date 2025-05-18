package com.kvmvkxnt.patientservice.controller;

import com.kvmvkxnt.patientservice.dto.PatientRequestDTO;
import com.kvmvkxnt.patientservice.dto.PatientResponseDTO;
import com.kvmvkxnt.patientservice.service.PatientService;
import com.kvmvkxnt.patientservice.validators.CreatePatientValidationGroup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/patients")
@Tag(name = "Patient", description = "API for managing patients")
public class PatientController {
  private final PatientService patientService;

  public PatientController(PatientService patientService) {
    this.patientService = patientService;
  }

  @GetMapping
  @Operation(summary = "Get all patients", description = "Retrieve a list of all patients")
  public ResponseEntity<List<PatientResponseDTO>> getPatients() {
    List<PatientResponseDTO> patients = patientService.getPatients();
    return ResponseEntity.ok().body(patients);
  }

  @PostMapping
  @Operation(summary = "Create a new patient", description = "Create a new patient record")
  public ResponseEntity<PatientResponseDTO> createPatient(
      @Validated({Default.class, CreatePatientValidationGroup.class}) @RequestBody
          PatientRequestDTO patientRequestDTO) {
    PatientResponseDTO createdPatient = patientService.createPatient(patientRequestDTO);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdPatient);
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Update an existing patient",
      description = "Update the details of an existing patient")
  public ResponseEntity<PatientResponseDTO> updatePatient(
      @PathVariable UUID id,
      @Validated({Default.class}) @RequestBody PatientRequestDTO patientRequestDTO) {
    PatientResponseDTO updatedPatient = patientService.updatePatient(id, patientRequestDTO);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).body(updatedPatient);
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a patient", description = "Delete a patient record by ID")
  public ResponseEntity<Void> deletePatient(@PathVariable UUID id) {
    patientService.deletePatient(id);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
