package parking.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import parking.dto.ViolationMessage;
import parking.entity.ParkingSlot;
import parking.entity.ParkingViolation;
import parking.entity.ViolationType;
import parking.repository.ParkingSlotRepository;
import parking.repository.ParkingViolationRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViolationDetectionService {
    
    private final ParkingSlotRepository slotRepository;
    private final ParkingViolationRepository violationRepository;
    private final KafkaProducerService kafkaProducer;
    
    @Value("${parking.max-stay-minutes}")
    private int maxStayMinutes;
    
    @Scheduled(fixedDelayString = "${parking.violation-check-interval}")
    @Transactional
    public void detectOverstayViolations() {
        log.info("========== STARTING VIOLATION DETECTION CHECK ==========");
        log.info("Checking for vehicles exceeding max stay duration of {} minutes", maxStayMinutes);
        
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(maxStayMinutes);
        log.debug("Threshold timestamp: {}", threshold);
        
        List<ParkingSlot> violatingSlots = slotRepository.findOccupiedSlotsOlderThan(threshold);
        
        log.info("Found {} potential overstay violations", violatingSlots.size());
        
        if (violatingSlots.isEmpty()) {
            log.info("No violations detected at this time");
        }
        
        for (ParkingSlot slot : violatingSlots) {
            log.info("Checking slot: {} occupied by vehicle: {} since {}", 
                slot.getSlotNumber(), slot.getCurrentVehicleNumber(), slot.getOccupiedSince());
            
            // Check if violation already exists
            boolean alreadyReported = violationRepository.existsBySlotNumberAndVehicleNumberAndResolvedFalse(
                slot.getSlotNumber(), 
                slot.getCurrentVehicleNumber()
            );
            
            if (!alreadyReported) {
                log.warn("NEW VIOLATION DETECTED - Creating violation record for slot: {}", 
                    slot.getSlotNumber());
                createViolation(slot);
            } else {
                log.debug("Violation already reported for slot: {} - Skipping", slot.getSlotNumber());
            }
        }
        
        log.info("========== VIOLATION DETECTION CHECK COMPLETED ==========");
    }
    
    @Transactional
    public void createViolation(ParkingSlot slot) {
        long overstayMinutes = Duration.between(slot.getOccupiedSince(), LocalDateTime.now()).toMinutes();
        long actualOverstay = overstayMinutes - maxStayMinutes;
        
        log.warn("╔════════════════════════════════════════════════════════════╗");
        log.warn("║           OVERSTAY VIOLATION DETECTED                      ║");
        log.warn("╠════════════════════════════════════════════════════════════╣");
        log.warn("║ Slot Number:      {}                                      ", slot.getSlotNumber());
        log.warn("║ Vehicle Number:   {}                                   ", slot.getCurrentVehicleNumber());
        log.warn("║ Entry Time:       {}                           ", slot.getOccupiedSince());
        log.warn("║ Total Duration:   {} minutes                              ", overstayMinutes);
        log.warn("║ Max Allowed:      {} minutes                              ", maxStayMinutes);
        log.warn("║ Overstay:         {} minutes                              ", actualOverstay);
        log.warn("╚════════════════════════════════════════════════════════════╝");
        
        // Create violation record
        ParkingViolation violation = new ParkingViolation();
        violation.setSlotNumber(slot.getSlotNumber());
        violation.setVehicleNumber(slot.getCurrentVehicleNumber());
        violation.setViolationType(ViolationType.OVERSTAY);
        violation.setDetectedAt(LocalDateTime.now());
        violation.setEntryTime(slot.getOccupiedSince());
        violation.setOverstayMinutes(actualOverstay);
        violation.setResolved(false);
        
        ParkingViolation savedViolation = violationRepository.save(violation);
        log.info("Violation record created with ID: {}", savedViolation.getId());
        
        // Calculate severity
        String severity = calculateSeverity(actualOverstay);
        log.warn("Violation severity: {}", severity);
        
        // Send violation alert to Kafka
        ViolationMessage message = new ViolationMessage(
            savedViolation.getId(),
            savedViolation.getSlotNumber(),
            savedViolation.getVehicleNumber(),
            savedViolation.getViolationType().toString(),
            savedViolation.getDetectedAt(),
            savedViolation.getOverstayMinutes(),
            severity
        );
        
        kafkaProducer.sendViolationAlert(message);
        log.warn("Violation alert sent to Kafka for vehicle: {}", savedViolation.getVehicleNumber());
    }
    
    private String calculateSeverity(long overstayMinutes) {
        if (overstayMinutes < 15) {
            log.debug("Severity calculated as LOW for {} minutes overstay", overstayMinutes);
            return "LOW";
        }
        if (overstayMinutes < 60) {
            log.debug("Severity calculated as MEDIUM for {} minutes overstay", overstayMinutes);
            return "MEDIUM";
        }
        log.debug("Severity calculated as HIGH for {} minutes overstay", overstayMinutes);
        return "HIGH";
    }
    
    @Transactional
    public void resolveViolation(Long violationId, String notes) {
        log.info("Resolving violation with ID: {}", violationId);
        
        ParkingViolation violation = violationRepository.findById(violationId)
            .orElseThrow(() -> {
                log.error("Violation not found with ID: {}", violationId);
                return new RuntimeException("Violation not found");
            });
        
        log.info("Found violation - Vehicle: {}, Slot: {}, Overstay: {} minutes", 
            violation.getVehicleNumber(), violation.getSlotNumber(), violation.getOverstayMinutes());
        
        violation.setResolved(true);
        violation.setResolvedAt(LocalDateTime.now());
        violation.setNotes(notes);
        
        violationRepository.save(violation);
        log.info("Violation {} resolved successfully at {}. Notes: {}", 
            violationId, violation.getResolvedAt(), notes);
    }
    
    public List<ParkingViolation> getActiveViolations() {
        log.debug("Fetching all active violations");
        List<ParkingViolation> violations = violationRepository.findByResolvedFalseOrderByDetectedAtDesc();
        log.info("Found {} active violations", violations.size());
        return violations;
    }
}