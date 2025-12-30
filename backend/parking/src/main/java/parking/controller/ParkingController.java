package parking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import parking.dto.CreateSlotRequest;
import parking.dto.ParkingEventRequest;
import parking.dto.ParkingEventResponse;
import parking.dto.ParkingStatsResponse;
import parking.entity.ParkingEvent;
import parking.entity.ParkingSlot;
import parking.repository.ParkingEventRepository;
import parking.repository.ParkingSlotRepository;
import parking.services.KafkaProducerService;
import parking.services.ParkingService;

import java.util.List;

@RestController
@RequestMapping("/api/parking")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ParkingController {
    
    private final ParkingService parkingService;
    private final KafkaProducerService kafkaProducer;
    private final ParkingSlotRepository slotRepository;
    private final ParkingEventRepository eventRepository;
    
    @PostMapping("/events")
    public ResponseEntity<ParkingEventResponse> createEvent(@Valid @RequestBody ParkingEventRequest request) {
        log.info("=== Received parking event request ===");
        log.info("Slot: {}, Vehicle: {}, Type: {}", 
            request.getSlotNumber(), request.getVehicleNumber(), request.getEventType());
        
        try {
            // Send to Kafka for async processing
            kafkaProducer.sendParkingEvent(request);
            
            // Also process synchronously for immediate response
            ParkingEventResponse response;
            if ("ENTRY".equalsIgnoreCase(request.getEventType())) {
                response = parkingService.handleEntry(request);
            } else if ("EXIT".equalsIgnoreCase(request.getEventType())) {
                response = parkingService.handleExit(request);
            } else {
                log.error("Invalid event type: {}", request.getEventType());
                throw new IllegalArgumentException("Invalid event type: " + request.getEventType());
            }
            
            log.info("Event processed successfully: {}", response.getMessage());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("Error processing parking event: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @PostMapping("/slots")
    public ResponseEntity<ParkingSlot> createSlot(@Valid @RequestBody CreateSlotRequest request) {
        log.info("Creating new parking slot: {}", request.getSlotNumber());
        ParkingSlot slot = parkingService.createSlot(request);
        log.info("Slot created successfully: {}", slot.getSlotNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(slot);
    }
    
    @GetMapping("/slots")
    public ResponseEntity<List<ParkingSlot>> getAllSlots() {
        log.debug("Fetching all parking slots");
        List<ParkingSlot> slots = slotRepository.findAll();
        log.info("Retrieved {} parking slots", slots.size());
        return ResponseEntity.ok(slots);
    }
    
    @GetMapping("/slots/{slotNumber}")
    public ResponseEntity<ParkingSlot> getSlot(@PathVariable String slotNumber) {
        log.debug("Fetching slot: {}", slotNumber);
        return slotRepository.findBySlotNumber(slotNumber)
            .map(slot -> {
                log.info("Found slot: {} with status: {}", slot.getSlotNumber(), slot.getStatus());
                return ResponseEntity.ok(slot);
            })
            .orElseGet(() -> {
                log.warn("Slot not found: {}", slotNumber);
                return ResponseEntity.notFound().build();
            });
    }
    
    @GetMapping("/events")
    public ResponseEntity<List<ParkingEvent>> getAllEvents() {
        log.debug("Fetching all parking events");
        List<ParkingEvent> events = eventRepository.findAll();
        log.info("Retrieved {} parking events", events.size());
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/events/slot/{slotNumber}")
    public ResponseEntity<List<ParkingEvent>> getEventsBySlot(@PathVariable String slotNumber) {
        log.debug("Fetching events for slot: {}", slotNumber);
        List<ParkingEvent> events = eventRepository.findBySlotNumberOrderByTimestampDesc(slotNumber);
        log.info("Found {} events for slot: {}", events.size(), slotNumber);
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<ParkingStatsResponse> getStats() {
        log.info("Fetching parking statistics");
        ParkingStatsResponse stats = parkingService.getStats();
        return ResponseEntity.ok(stats);
    }
}