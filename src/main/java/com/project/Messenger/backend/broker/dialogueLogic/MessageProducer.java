package com.project.Messenger.backend.broker.dialogueLogic;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

@Slf4j
public class MessageProducer {
    private final KafkaProducer<String, String> producer;
    private final String kafkaBootstrapServers = "localhost:9093";
    private final String createTopicsRule = "auto.create.topics.enable";

    public MessageProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        props.put(createTopicsRule, "true");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, "10485760");

        this.producer = new KafkaProducer<>(props, new StringSerializer(), new StringSerializer());
    }

    public void sendMessage(String topic, String message) {
        try {
            producer.send(new ProducerRecord<>(topic, message));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        producer.close();
    }
}