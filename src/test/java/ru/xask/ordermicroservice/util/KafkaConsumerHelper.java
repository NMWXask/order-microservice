package ru.xask.ordermicroservice.util;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.testcontainers.containers.KafkaContainer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

public final class KafkaConsumerHelper {

    private KafkaConsumerHelper() {}

    public static Consumer<String, String> createConsumer(KafkaContainer kafka, String topic) {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + System.currentTimeMillis(),
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
        );
        DefaultKafkaConsumerFactory<String, String> factory = new DefaultKafkaConsumerFactory<>(props);
        Consumer<String, String> consumer = factory.createConsumer();
        consumer.subscribe(List.of(topic));
        return consumer;
    }

    public static ConsumerRecords<String, String> pollRecords(Consumer<String, String> consumer, Duration timeout) {
        return KafkaTestUtils.getRecords(consumer, timeout);
    }

    public static boolean containsMessage(ConsumerRecords<String, String> records, String topic, String expectedValue) {
        return StreamSupport.stream(records.records(topic).spliterator(), false)
                .anyMatch(record -> record.value().equals(expectedValue));
    }

    public static boolean waitForMessage(KafkaContainer kafka, String topic, String expectedValue, Duration timeout) { // паблик статичный метод который  возвращает булевое значение,контейнер,название топика,ожидаемое значение,таймут
        try (Consumer<String, String> consumer = createConsumer(kafka, topic)) { // создается консюиер,
            ConsumerRecords<String, String> records = pollRecords(consumer, timeout);// получение сообщение
            return containsMessage(records, topic, expectedValue); // либо получено сообщение либо нет
        }
    }
}