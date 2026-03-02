package com.bank.transactionservice.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TransactionEventProducer {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public TransactionEventProducer(KafkaTemplate<String, TransactionEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(TransactionEvent event) {
        try {
            SendResult<String, TransactionEvent> result =
                    kafkaTemplate.send("transaction-events", event).get();

            log.info("[TransactionEventProducer][Partition]: {}", result.getRecordMetadata().partition());
            log.info("[TransactionEventProducer][Offset]: {}", result.getRecordMetadata().offset());
            log.info("[TransactionEventProducer][Kafka event published successfully]: {}", event);

        } catch (Exception e) {
            log.error("[TransactionEventProducer][Failed to publish Kafka event]: {}", e.getMessage());
        }
    }
}