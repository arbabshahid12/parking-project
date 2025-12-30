package parking.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${parking.topics.parking-events}")
    private String parkingEventsTopic;

    @Value("${parking.topics.slot-updates}")
    private String slotUpdatesTopic;

    @Value("${parking.topics.violations}")
    private String violationsTopic;

    // PRODUCER CONFIGURATION
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        log.info("Configuring Kafka Producer with bootstrap servers: {}", bootstrapServers);
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);  // Use String only
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        log.info("Creating Kafka Template for message production");
        return new KafkaTemplate<>(producerFactory());
    }

    // ============ CONSUMER CONFIGURATION ============
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        log.info("Configuring Kafka Consumer with bootstrap servers: {}", bootstrapServers);
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "parking-consumer-group");
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        log.info("Creating Kafka Listener Container Factory");
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setPollTimeout(3000);

        return factory;
    }

    // ============ TOPIC DEFINITIONS ============
    @Bean
    public NewTopic parkingEventsTopic() {
        log.info("Creating Kafka topic: {}", parkingEventsTopic);
        return TopicBuilder.name(parkingEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic slotUpdatesTopic() {
        log.info("Creating Kafka topic: {}", slotUpdatesTopic);
        return TopicBuilder.name(slotUpdatesTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic violationsTopic() {
        log.info("Creating Kafka topic: {}", violationsTopic);
        return TopicBuilder.name(violationsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}