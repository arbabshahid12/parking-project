package parking.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import parking.dto.ParkingEventRequest;
import parking.dto.SlotUpdateMessage;
import parking.dto.ViolationMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {
    
    private final ParkingService parkingService;
    private final SSEService sseService;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(topics = "${parking.topics.parking-events}", groupId = "parking-consumer-group")
    public void consumeParkingEvent(String message) {
        try {
            log.info("Received parking event from Kafka: {}", message);
            ParkingEventRequest event = objectMapper.readValue(message, ParkingEventRequest.class);
            log.debug("Parsed parking event - Slot: {}, Vehicle: {}, Type: {}", 
                event.getSlotNumber(), event.getVehicleNumber(), event.getEventType());
            parkingService.processEvent(event);
            log.info("Successfully processed parking event for slot: {}", event.getSlotNumber());
        } catch (Exception e) {
            log.error("Error processing parking event: {}. Exception: {}", 
                message, e.getMessage(), e);
        }
    }
    
    @KafkaListener(topics = "${parking.topics.slot-updates}", groupId = "parking-consumer-group")
    public void consumeSlotUpdate(String message) {
        try {
            log.info("Received slot update from Kafka: {}", message);
            SlotUpdateMessage update = objectMapper.readValue(message, SlotUpdateMessage.class);
            log.debug("Parsed slot update - Slot: {}, Status: {}, Vehicle: {}", 
                update.getSlotNumber(), update.getStatus(), update.getVehicleNumber());
            
            // Broadcast to SSE clients
            sseService.broadcastSlotUpdate(update);
            log.info("Broadcasted slot update to SSE clients for slot: {}", update.getSlotNumber());
        } catch (Exception e) {
            log.error("Error processing slot update: {}. Exception: {}", 
                message, e.getMessage(), e);
        }
    }
    
    @KafkaListener(topics = "${parking.topics.violations}", groupId = "parking-consumer-group")
    public void consumeViolation(String message) {
        try {
            log.warn("Received violation alert from Kafka: {}", message);
            ViolationMessage violation = objectMapper.readValue(message, ViolationMessage.class);
            log.warn("Parsed violation - Vehicle: {}, Slot: {}, Overstay: {} minutes", 
                violation.getVehicleNumber(), violation.getSlotNumber(), 
                violation.getOverstayMinutes());
            
            // Broadcast to SSE clients
            sseService.broadcastViolation(violation);
            log.warn("Broadcasted violation alert to SSE clients for vehicle: {}", 
                violation.getVehicleNumber());
        } catch (Exception e) {
            log.error("Error processing violation: {}. Exception: {}", 
                message, e.getMessage(), e);
        }
    }
}