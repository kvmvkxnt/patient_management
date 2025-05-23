package com.kvmvkxnt.patientservice.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumer {

  private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

  @KafkaListener(topics = "patient.response", groupId = "patient-service")
  public void consumeEvent(byte[] event) {
    try {
    } catch (Exception e) {
      // TODO: handle exception
    }
    log.info("Received event: {}", new String(event));
  }
}
