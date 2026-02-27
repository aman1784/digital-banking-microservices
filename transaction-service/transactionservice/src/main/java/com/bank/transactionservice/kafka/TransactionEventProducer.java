package com.bank.transactionservice.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

@Service
public class TransactionEventProducer {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public TransactionEventProducer(KafkaTemplate<String, TransactionEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(TransactionEvent event) {
        try {
            SendResult<String, TransactionEvent> result =
                    kafkaTemplate.send("transaction-events", event).get();

            System.out.println("Partition: " + result.getRecordMetadata().partition());
            System.out.println("Offset: " + result.getRecordMetadata().offset());
            kafkaTemplate
                    .send("transaction-events", event)
                    .get(); // wait for broker acknowledgment



            System.out.println("Kafka event published successfully: " + event);

        } catch (Exception e) {
            System.err.println("Failed to publish Kafka event: " + e.getMessage());
        }
    }
}