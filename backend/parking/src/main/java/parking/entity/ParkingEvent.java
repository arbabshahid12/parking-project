package parking.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Entity
@Table(name = "parking_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@Getter
@Setter

public class ParkingEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String slotNumber;
    
    @Column(nullable = false)
    private String vehicleNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column
    private Long durationMinutes;
    
    @Column
    private String additionalInfo;
    
    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        log.info("Creating parking event: {} for vehicle {} at slot {}", 
                 eventType, vehicleNumber, slotNumber);
    }
}