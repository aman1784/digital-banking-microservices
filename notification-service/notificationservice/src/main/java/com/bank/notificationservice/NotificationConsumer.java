package com.bank.notificationservice;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationConsumer {

    @KafkaListener(topics = "transaction-events", groupId = "notification-group-final")
    public void consume(TransactionEvent event) {
        System.out.println("📩 Notification Service received: " + event);
    }

    @KafkaListener(topics = "transaction-events", groupId = "debug-group")
    public void consume(String message) {
        System.out.println("RAW MESSAGE: " + message);
    }
}