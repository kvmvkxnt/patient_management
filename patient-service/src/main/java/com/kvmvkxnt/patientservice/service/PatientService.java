package com.kvmvkxnt.patientservice.service;

import com.kvmvkxnt.patientservice.dto.PatientRequestDTO;
import com.kvmvkxnt.patientservice.dto.PatientResponseDTO;
import com.kvmvkxnt.patientservice.exception.EmailAlreadyExistsException;
import com.kvmvkxnt.patientservice.exception.PatientNotFoundException;
import com.kvmvkxnt.patientservice.grpc.BillingServiceGrpcClient;
import com.kvmvkxnt.patientservice.mapper.PatientMapper;
import com.kvmvkxnt.patientservice.model.Patient;
import com.kvmvkxnt.patientservice.repository.PatientRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.kvmvkxnt.patientservice.kafka.KafkaProducer;
import org.springframework.stereotype.Service;

@Service
public class PatientService {
  private final PatientRepository patientRepository;
  private final BillingServiceGrpcClient billingServiceGrpcClient;
  private final KafkaProducer kafkaProducer;

  public PatientService(
      PatientRepository patientRepository, BillingServiceGrpcClient billingServiceGrpcClient, KafkaProducer kafkaProducer) {
    this.patientRepository = patientRepository;
    this.billingServiceGrpcClient = billingServiceGrpcClient;
    this.kafkaProducer = kafkaProducer;
  }

  public List<PatientResponseDTO> getPatients() {
    List<Patient> patients = patientRepository.findAll();
    return patients.stream().map(PatientMapper::toDTO).toList();
  }

  public PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO) {
    if (patientRepository.existsByEmail(patientRequestDTO.getEmail())) {
      throw new EmailAlreadyExistsException(
          "A patient with this email already exists " + patientRequestDTO.getEmail());
    }
    Patient newPatient = patientRepository.save(PatientMapper.toEntity(patientRequestDTO));

    billingServiceGrpcClient.createBillingAccount(
        newPatient.getId().toString(), newPatient.getName(), newPatient.getEmail());

    kafkaProducer.sendEvent(newPatient);

    return PatientMapper.toDTO(newPatient);
  }

  public PatientResponseDTO updatePatient(UUID id, PatientRequestDTO patientRequestDTO) {
    Patient patient =
        patientRepository
            .findById(id)
            .orElseThrow(() -> new PatientNotFoundException("Patient not found with ID: " + id));

    if (patientRepository.existsByEmailAndIdNot(patientRequestDTO.getEmail(), id)) {
      throw new EmailAlreadyExistsException(
          "A patient with this email already exists " + patientRequestDTO.getEmail());
    }

    patient.setName(patientRequestDTO.getName());
    patient.setEmail(patientRequestDTO.getEmail());
    patient.setAddress(patientRequestDTO.getAddress());
    patient.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));

    Patient updatedPatient = patientRepository.save(patient);

    return PatientMapper.toDTO(updatedPatient);
  }

  public void deletePatient(UUID id) {
    Patient patient =
        patientRepository
            .findById(id)
            .orElseThrow(() -> new PatientNotFoundException("Patient not found with ID: " + id));
    patientRepository.delete(patient);
  }
}
