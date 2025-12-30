package parking.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import parking.entity.ParkingViolation;

import java.util.List;

@Repository
public interface ParkingViolationRepository extends JpaRepository<ParkingViolation, Long> {
    
    List<ParkingViolation> findByResolvedFalseOrderByDetectedAtDesc();
    
    List<ParkingViolation> findByVehicleNumberOrderByDetectedAtDesc(String vehicleNumber);
    
    List<ParkingViolation> findBySlotNumberOrderByDetectedAtDesc(String slotNumber);
    
    @Query("SELECT COUNT(v) FROM ParkingViolation v WHERE v.resolved = false")
    Long countUnresolvedViolations();
    
    boolean existsBySlotNumberAndVehicleNumberAndResolvedFalse(String slotNumber, String vehicleNumber);
}