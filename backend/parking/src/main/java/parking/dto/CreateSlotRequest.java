package parking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSlotRequest {
    
    @NotBlank(message = "Slot number is required")
    private String slotNumber;
    
    private String floor;
    
    private String zone;
    
    private Boolean reserved = false;
    
    private Integer maxStayMinutes = 30;
}