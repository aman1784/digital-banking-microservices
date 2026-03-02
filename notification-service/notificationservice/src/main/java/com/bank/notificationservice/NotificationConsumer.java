package com.bank.notificationservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationConsumer {

    @KafkaListener(topics = "transaction-events", groupId = "notification-group-final")
    public void consume(TransactionEvent event) {
        log.info("[NotificationConsumer][EVENT]: {}", event);
    }

//    @KafkaListener(topics = "transaction-events", groupId = "debug-group")
//    public void consume(String message) {
//        log.info("[NotificationConsumer][RAW MESSAGE]: {}", message);
//    }
}