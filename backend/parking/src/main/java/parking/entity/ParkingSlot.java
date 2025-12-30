package parking.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Entity
@Table(name = "parking_slots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@Setter
@Getter
public class ParkingSlot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String slotNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SlotStatus status = SlotStatus.VACANT;
    
    @Column(nullable = false)
    private Boolean reserved = false;
    
    @Column(nullable = false)
    private Integer maxStayMinutes = 30;
    
    @Column
    private String floor;
    
    @Column
    private String zone;
    
    @Column
    private LocalDateTime lastUpdated;
    
    @Column
    private String currentVehicleNumber;
    
    @Column
    private LocalDateTime occupiedSince;
    
    @PreUpdate
    @PrePersist
    public void updateTimestamp() {
        this.lastUpdated = LocalDateTime.now();
        log.debug("Updated timestamp for slot: {} at {}", slotNumber, lastUpdated);
    }
}