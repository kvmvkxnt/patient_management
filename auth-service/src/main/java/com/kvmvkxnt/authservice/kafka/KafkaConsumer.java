package com.kvmvkxnt.authservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Service
public class KafkaConsumer {

  private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

  @KafkaListener(topics = "patient.request", groupId = "auth-service")
  public void consumeEvent(byte[] event) {
    try {
      PatientEvent patientEvent = PatientEvent.parseFrom(event);
      log.info(
          "Received PatientEvent: [PatientId={}, PatientName={}, PatientUsername={},"
              + " PatientEmail={} ]",
          patientEvent.getPatientId(),
          patientEvent.getName(),
          patientEvent.getUsername(),
          patientEvent.getEmail());

    } catch (InvalidProtocolBufferException e) {
      log.error("Error deserializing PatientEvent: {}", e.getMessage());
    }
  }
}
