package parking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingStatsResponse {
    
    private Long totalSlots;
    
    private Long availableSlots;
    
    private Long occupiedSlots;
    
    private Long activeViolations;
    
    private Double occupancyRate;
}