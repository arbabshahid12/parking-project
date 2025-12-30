package parking.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import parking.entity.ParkingSlot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {
    
    Optional<ParkingSlot> findBySlotNumber(String slotNumber);
    
    List<ParkingSlot> findByStatus(String status);
    
    @Query("SELECT p FROM ParkingSlot p WHERE p.status = 'OCCUPIED' AND p.occupiedSince < :threshold")
    List<ParkingSlot> findOccupiedSlotsOlderThan(LocalDateTime threshold);
    
    @Query("SELECT COUNT(p) FROM ParkingSlot p WHERE p.status = 'VACANT'")
    Long countAvailableSlots();
    
    List<ParkingSlot> findByFloorAndZone(String floor, String zone);
}