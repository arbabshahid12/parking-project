package parking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ParkingEventRequest {
    
    @NotBlank(message = "Slot number is required")
    private String slotNumber;
    
    @NotBlank(message = "Vehicle number is required")
    private String vehicleNumber;
    
    @NotBlank(message = "Event type is required (ENTRY or EXIT)")
    private String eventType;
    
    private String additionalInfo;
}