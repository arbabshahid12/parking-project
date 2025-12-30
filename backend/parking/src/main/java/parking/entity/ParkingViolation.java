package parking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Entity
@Table(name = "parking_violations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class ParkingViolation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String slotNumber;
    
    @Column(nullable = false)
    private String vehicleNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ViolationType violationType;
    
    @Column(nullable = false)
    private LocalDateTime detectedAt;
    
    @Column(nullable = false)
    private Long overstayMinutes;
    
    @Column
    private LocalDateTime entryTime;
    
    @Column
    private Boolean resolved = false;
    
    @Column
    private LocalDateTime resolvedAt;
    
    @Column
    private String notes;
    
    @PrePersist
    public void prePersist() {
        if (detectedAt == null) {
            detectedAt = LocalDateTime.now();
        }
        log.warn("Recording violation: {} for vehicle {} in slot {} - Overstay: {} minutes", 
                 violationType, vehicleNumber, slotNumber, overstayMinutes);
    }
}