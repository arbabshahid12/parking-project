package parking.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import parking.dto.*;
import parking.entity.EventType;
import parking.entity.ParkingEvent;
import parking.entity.ParkingSlot;
import parking.entity.SlotStatus;
import parking.repository.ParkingEventRepository;
import parking.repository.ParkingSlotRepository;
import parking.repository.ParkingViolationRepository;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingService {
    
    private final ParkingSlotRepository slotRepository;
    private final ParkingEventRepository eventRepository;
    private final ParkingViolationRepository violationRepository;
    private final KafkaProducerService kafkaProducer;
    
    @Value("${parking.max-stay-minutes}")
    private int maxStayMinutes;
    
    @Transactional
    public ParkingEventResponse handleEntry(ParkingEventRequest request) {
        log.info("Processing ENTRY event - Vehicle: {}, Slot: {}", 
            request.getVehicleNumber(), request.getSlotNumber());
        
        ParkingSlot slot = slotRepository.findBySlotNumber(request.getSlotNumber())
            .orElseThrow(() -> {
                log.error("Slot not found: {}", request.getSlotNumber());
                return new RuntimeException("Slot not found: " + request.getSlotNumber());
            });
        
        log.debug("Found slot: {} with status: {}", slot.getSlotNumber(), slot.getStatus());
        
        if (!"VACANT".equals(slot.getStatus().toString())) {
            log.error("Slot {} is not available. Current status: {}", 
                slot.getSlotNumber(), slot.getStatus());
            throw new RuntimeException("Slot is not available");
        }
        
        // Update slot status
        slot.setStatus(SlotStatus.OCCUPIED);
        slot.setCurrentVehicleNumber(request.getVehicleNumber());
        slot.setOccupiedSince(LocalDateTime.now());
        slotRepository.save(slot);
        log.info("Slot {} marked as OCCUPIED for vehicle: {} at {}", 
            slot.getSlotNumber(), request.getVehicleNumber(), slot.getOccupiedSince());
        
        // Create parking event
        ParkingEvent event = new ParkingEvent();
        event.setSlotNumber(request.getSlotNumber());
        event.setVehicleNumber(request.getVehicleNumber());
        event.setEventType(EventType.ENTRY);
        event.setTimestamp(LocalDateTime.now());
        event.setAdditionalInfo(request.getAdditionalInfo());
        eventRepository.save(event);
        log.info("Created ENTRY event with ID: {} for vehicle: {}", 
            event.getId(), event.getVehicleNumber());
        
        // Send slot update to Kafka
        SlotUpdateMessage update = new SlotUpdateMessage(
            slot.getSlotNumber(),
            slot.getStatus().toString(),
            slot.getCurrentVehicleNumber(),
            LocalDateTime.now(),
            0L,
            slot.getFloor(),
            slot.getZone()
        );
        kafkaProducer.sendSlotUpdate(update);
        
        return new ParkingEventResponse(
            event.getId(),
            event.getSlotNumber(),
            event.getVehicleNumber(),
            event.getEventType().toString(),
            event.getTimestamp(),
            null,
            "Vehicle entry recorded successfully"
        );
    }
    
    @Transactional
    public ParkingEventResponse handleExit(ParkingEventRequest request) {
        log.info("Processing EXIT event - Vehicle: {}, Slot: {}", 
            request.getVehicleNumber(), request.getSlotNumber());
        
        ParkingSlot slot = slotRepository.findBySlotNumber(request.getSlotNumber())
            .orElseThrow(() -> {
                log.error("Slot not found: {}", request.getSlotNumber());
                return new RuntimeException("Slot not found: " + request.getSlotNumber());
            });
        
        log.debug("Found slot: {} with status: {}", slot.getSlotNumber(), slot.getStatus());
        
        if (!"OCCUPIED".equals(slot.getStatus().toString())) {
            log.error("Slot {} is not occupied. Current status: {}", 
                slot.getSlotNumber(), slot.getStatus());
            throw new RuntimeException("Slot is not occupied");
        }
        
        // Calculate duration
        Long duration = null;
        if (slot.getOccupiedSince() != null) {
            duration = Duration.between(slot.getOccupiedSince(), LocalDateTime.now()).toMinutes();
            log.info("Vehicle {} stayed in slot {} for {} minutes", 
                request.getVehicleNumber(), slot.getSlotNumber(), duration);
        }
        
        // Update slot status
        slot.setStatus(SlotStatus.VACANT);
        slot.setCurrentVehicleNumber(null);
        slot.setOccupiedSince(null);
        slotRepository.save(slot);
        log.info("Slot {} marked as VACANT", slot.getSlotNumber());
        
        // Create parking event
        ParkingEvent event = new ParkingEvent();
        event.setSlotNumber(request.getSlotNumber());
        event.setVehicleNumber(request.getVehicleNumber());
        event.setEventType(EventType.EXIT);
        event.setTimestamp(LocalDateTime.now());
        event.setDurationMinutes(duration);
        event.setAdditionalInfo(request.getAdditionalInfo());
        eventRepository.save(event);
        log.info("Created EXIT event with ID: {} for vehicle: {} with duration: {} minutes", 
            event.getId(), event.getVehicleNumber(), duration);
        
        // Send slot update to Kafka
        SlotUpdateMessage update = new SlotUpdateMessage(
            slot.getSlotNumber(),
            slot.getStatus().toString(),
            null,
            LocalDateTime.now(),
            duration,
            slot.getFloor(),
            slot.getZone()
        );
        kafkaProducer.sendSlotUpdate(update);
        
        return new ParkingEventResponse(
            event.getId(),
            event.getSlotNumber(),
            event.getVehicleNumber(),
            event.getEventType().toString(),
            event.getTimestamp(),
            duration,
            "Vehicle exit recorded successfully. Duration: " + duration + " minutes"
        );
    }
    
    @Transactional
    public void processEvent(ParkingEventRequest request) {
        log.info("Processing event - Type: {}, Slot: {}, Vehicle: {}", 
            request.getEventType(), request.getSlotNumber(), request.getVehicleNumber());
        
        if ("ENTRY".equalsIgnoreCase(request.getEventType())) {
            handleEntry(request);
        } else if ("EXIT".equalsIgnoreCase(request.getEventType())) {
            handleExit(request);
        } else {
            log.error("Invalid event type: {}", request.getEventType());
            throw new RuntimeException("Invalid event type: " + request.getEventType());
        }
    }
    
    @Transactional
    public ParkingSlot createSlot(CreateSlotRequest request) {
        log.info("Creating new parking slot: {}", request.getSlotNumber());
        
        if (slotRepository.findBySlotNumber(request.getSlotNumber()).isPresent()) {
            log.error("Slot already exists: {}", request.getSlotNumber());
            throw new RuntimeException("Slot already exists: " + request.getSlotNumber());
        }
        
        ParkingSlot slot = new ParkingSlot();
        slot.setSlotNumber(request.getSlotNumber());
        slot.setFloor(request.getFloor());
        slot.setZone(request.getZone());
        slot.setReserved(request.getReserved());
        slot.setMaxStayMinutes(request.getMaxStayMinutes());
        slot.setStatus(SlotStatus.VACANT);
        
        ParkingSlot savedSlot = slotRepository.save(slot);
        log.info("Successfully created parking slot: {} on floor: {}, zone: {}", 
            savedSlot.getSlotNumber(), savedSlot.getFloor(), savedSlot.getZone());
        
        return savedSlot;
    }
    
    public ParkingStatsResponse getStats() {
        log.debug("Fetching parking statistics");
        
        long total = slotRepository.count();
        long available = slotRepository.countAvailableSlots();
        long occupied = total - available;
        long violations = violationRepository.countUnresolvedViolations();
        double occupancyRate = total > 0 ? (occupied * 100.0 / total) : 0;
        
        log.info("Parking Stats - Total: {}, Available: {}, Occupied: {}, Violations: {}, Occupancy Rate: {}%", 
            total, available, occupied, violations, String.format("%.2f", occupancyRate));
        
        return new ParkingStatsResponse(total, available, occupied, violations, occupancyRate);
    }
}