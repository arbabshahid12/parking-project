package parking.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import parking.dto.SlotUpdateMessage;
import parking.dto.ViolationMessage;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;  // Changed to String, String
    private final ObjectMapper objectMapper;

    @Value("${parking.topics.parking-events}")
    private String parkingEventsTopic;

    @Value("${parking.topics.slot-updates}")
    private String slotUpdatesTopic;

    @Value("${parking.topics.violations}")
    private String violationsTopic;

    public void sendParkingEvent(Object event) {
        try {
            String jsonEvent = objectMapper.writeValueAsString(event);
            log.info("Sending parking event to Kafka topic: {}", parkingEventsTopic);

            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(parkingEventsTopic, jsonEvent);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("✓ Successfully sent parking event to topic: {} at offset: {}",
                            parkingEventsTopic, result.getRecordMetadata().offset());
                    log.debug("Event details: {}", jsonEvent);
                } else {
                    log.error("✗ Failed to send parking event to topic: {}. Error: {}",
                            parkingEventsTopic, ex.getMessage(), ex);
                }
            });
        } catch (Exception e) {
            log.error("✗ Error serializing parking event: {}", e.getMessage(), e);
        }
    }

    public void sendSlotUpdate(SlotUpdateMessage update) {
        try {
            String jsonUpdate = objectMapper.writeValueAsString(update);
            log.info("Sending slot update to Kafka - Slot: {}, Status: {}",
                    update.getSlotNumber(), update.getStatus());

            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(slotUpdatesTopic, update.getSlotNumber(), jsonUpdate);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("✓ Successfully sent slot update for slot: {} to offset: {}",
                            update.getSlotNumber(), result.getRecordMetadata().offset());
                } else {
                    log.error("✗ Failed to send slot update for slot: {}. Error: {}",
                            update.getSlotNumber(), ex.getMessage(), ex);
                }
            });
        } catch (Exception e) {
            log.error("✗ Error serializing slot update: {}", e.getMessage(), e);
        }
    }

    public void sendViolationAlert(ViolationMessage violation) {
        try {
            String jsonViolation = objectMapper.writeValueAsString(violation);
            log.warn("Sending violation alert to Kafka - Vehicle: {}, Slot: {}, Type: {}",
                    violation.getVehicleNumber(), violation.getSlotNumber(), violation.getViolationType());

            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(violationsTopic, violation.getSlotNumber(), jsonViolation);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.warn("✓ Successfully sent violation alert for vehicle: {} in slot: {} to offset: {}",
                            violation.getVehicleNumber(), violation.getSlotNumber(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("✗ Failed to send violation alert for vehicle: {} in slot: {}. Error: {}",
                            violation.getVehicleNumber(), violation.getSlotNumber(),
                            ex.getMessage(), ex);
                }
            });
        } catch (Exception e) {
            log.error("✗ Error serializing violation: {}", e.getMessage(), e);
        }
    }
}