package com.bank.transactionservice.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TransactionEventProducer {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public TransactionEventProducer(KafkaTemplate<String, TransactionEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(TransactionEvent event) {

        kafkaTemplate.send("transaction-events", event)
                .whenComplete((result, ex) -> {

                    if (ex != null) {
                        log.error("[TransactionEventProducer][publish] Kafka publish failed for transaction {}", ex.getMessage());
                        return;
                    }

                    log.info("[TransactionEventProducer][publish] Event published: partition={}, offset={}",
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                });
    }
}