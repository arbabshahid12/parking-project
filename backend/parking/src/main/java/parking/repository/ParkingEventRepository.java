package parking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import parking.entity.ParkingEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingEventRepository extends JpaRepository<ParkingEvent, Long> {
    
    List<ParkingEvent> findBySlotNumberOrderByTimestampDesc(String slotNumber);
    
    List<ParkingEvent> findByVehicleNumberOrderByTimestampDesc(String vehicleNumber);
    
    @Query("SELECT p FROM ParkingEvent p WHERE p.timestamp BETWEEN :start AND :end ORDER BY p.timestamp DESC")
    List<ParkingEvent> findEventsBetween(LocalDateTime start, LocalDateTime end);
    
    Optional<ParkingEvent> findFirstBySlotNumberAndEventTypeOrderByTimestampDesc(
        String slotNumber, String eventType);
}