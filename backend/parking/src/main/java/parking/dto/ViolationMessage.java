package parking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViolationMessage {
    
    private Long violationId;
    
    private String slotNumber;
    
    private String vehicleNumber;
    
    private String violationType;
    
    private LocalDateTime detectedAt;
    
    private Long overstayMinutes;
    
    private String severity;
}