package com.bank.notificationservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationConsumer {

    @KafkaListener(topics = "transaction-events", groupId = "notification-group")
    public void consume(TransactionEvent event) {
        log.info("[NotificationConsumer][EVENT]: {}", event);
    }

}