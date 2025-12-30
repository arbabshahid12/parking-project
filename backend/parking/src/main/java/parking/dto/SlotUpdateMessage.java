package parking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlotUpdateMessage {
    
    private String slotNumber;
    
    private String status;
    
    private String vehicleNumber;
    
    private LocalDateTime timestamp;
    
    private Long occupancyDuration;
    
    private String floor;
    
    private String zone;
}