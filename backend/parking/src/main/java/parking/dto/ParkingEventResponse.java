package parking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingEventResponse {
    
    private Long eventId;
    
    private String slotNumber;
    
    private String vehicleNumber;
    
    private String eventType;
    
    private LocalDateTime timestamp;
    
    private Long durationMinutes;
    
    private String message;
}