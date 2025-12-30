package parking.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import parking.entity.ParkingViolation;
import parking.repository.ParkingViolationRepository;
import parking.services.ViolationDetectionService;

import java.util.List;

@RestController
@RequestMapping("/api/violations")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ViolationController {
    
    private final ViolationDetectionService violationService;
    private final ParkingViolationRepository violationRepository;
    
    @GetMapping
    public ResponseEntity<List<ParkingViolation>> getAllViolations() {
        log.debug("Fetching all violations");
        List<ParkingViolation> violations = violationRepository.findAll();
        log.info("Retrieved {} total violations", violations.size());
        return ResponseEntity.ok(violations);
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<ParkingViolation>> getActiveViolations() {
        log.info("Fetching active violations");
        List<ParkingViolation> violations = violationService.getActiveViolations();
        return ResponseEntity.ok(violations);
    }
    
    @PostMapping("/{id}/resolve")
    public ResponseEntity<String> resolveViolation(
            @PathVariable Long id, 
            @RequestParam(required = false) String notes) {
        log.info("Resolving violation ID: {} with notes: {}", id, notes);
        violationService.resolveViolation(id, notes);
        return ResponseEntity.ok("Violation resolved successfully");
    }
    
    @PostMapping("/check")
    public ResponseEntity<String> triggerViolationCheck() {
        log.info("Manual violation check triggered");
        violationService.detectOverstayViolations();
        return ResponseEntity.ok("Violation check completed");
    }
}