package parking.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import parking.dto.SlotUpdateMessage;
import parking.dto.ViolationMessage;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class SSEService {
    
    private final List<SseEmitter> slotEmitters = new CopyOnWriteArrayList<>();
    private final List<SseEmitter> violationEmitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper;
    
    public SseEmitter createSlotEmitter() {
        log.info("Creating new SSE emitter for slot updates");
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        emitter.onCompletion(() -> {
            log.info("SSE slot emitter completed. Removing from active list");
            slotEmitters.remove(emitter);
            log.info("Active slot emitters: {}", slotEmitters.size());
        });
        
        emitter.onTimeout(() -> {
            log.warn("SSE slot emitter timed out. Removing from active list");
            slotEmitters.remove(emitter);
            log.info("Active slot emitters: {}", slotEmitters.size());
        });
        
        emitter.onError(e -> {
            log.error("SSE slot emitter error: {}. Removing from active list", e.getMessage());
            slotEmitters.remove(emitter);
            log.info("Active slot emitters: {}", slotEmitters.size());
        });
        
        slotEmitters.add(emitter);
        log.info("New SSE slot emitter created. Total active slot emitters: {}", slotEmitters.size());
        
        // Send initial connection message
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("{\"message\":\"Connected to slot updates stream\"}"));
            log.debug("Sent initial connection message to new slot emitter");
        } catch (IOException e) {
            log.error("Error sending initial connection message: {}", e.getMessage(), e);
        }
        
        return emitter;
    }
    
    public SseEmitter createViolationEmitter() {
        log.info("Creating new SSE emitter for violation updates");
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        emitter.onCompletion(() -> {
            log.info("SSE violation emitter completed. Removing from active list");
            violationEmitters.remove(emitter);
            log.info("Active violation emitters: {}", violationEmitters.size());
        });
        
        emitter.onTimeout(() -> {
            log.warn("SSE violation emitter timed out. Removing from active list");
            violationEmitters.remove(emitter);
            log.info("Active violation emitters: {}", violationEmitters.size());
        });
        
        emitter.onError(e -> {
            log.error("SSE violation emitter error: {}. Removing from active list", e.getMessage());
            violationEmitters.remove(emitter);
            log.info("Active violation emitters: {}", violationEmitters.size());
        });
        
        violationEmitters.add(emitter);
        log.info("New SSE violation emitter created. Total active violation emitters: {}", 
            violationEmitters.size());
        
        // Send initial connection message
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("{\"message\":\"Connected to violations stream\"}"));
            log.debug("Sent initial connection message to new violation emitter");
        } catch (IOException e) {
            log.error("Error sending initial connection message: {}", e.getMessage(), e);
        }
        
        return emitter;
    }
    
    public void broadcastSlotUpdate(SlotUpdateMessage update) {
        log.info("Broadcasting slot update to {} clients - Slot: {}, Status: {}", 
            slotEmitters.size(), update.getSlotNumber(), update.getStatus());
        
        if (slotEmitters.isEmpty()) {
            log.debug("No active slot emitters to broadcast to");
            return;
        }
        
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        int successCount = 0;
        
        for (SseEmitter emitter : slotEmitters) {
            try {
                String json = objectMapper.writeValueAsString(update);
                emitter.send(SseEmitter.event()
                    .name("slot-update")
                    .data(json));
                successCount++;
            } catch (IOException e) {
                log.error("Error sending slot update to client: {}. Marking for removal", 
                    e.getMessage());
                deadEmitters.add(emitter);
            }
        }
        
        slotEmitters.removeAll(deadEmitters);
        log.info("Slot update broadcast completed - Success: {}, Failed: {}, Active emitters: {}", 
            successCount, deadEmitters.size(), slotEmitters.size());
    }
    
    public void broadcastViolation(ViolationMessage violation) {
        log.warn("Broadcasting violation to {} clients - Vehicle: {}, Slot: {}", 
            violationEmitters.size(), violation.getVehicleNumber(), violation.getSlotNumber());
        
        if (violationEmitters.isEmpty()) {
            log.debug("No active violation emitters to broadcast to");
            return;
        }
        
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        int successCount = 0;
        
        for (SseEmitter emitter : violationEmitters) {
            try {
                String json = objectMapper.writeValueAsString(violation);
                emitter.send(SseEmitter.event()
                    .name("violation-alert")
                    .data(json));
                successCount++;
            } catch (IOException e) {
                log.error("Error sending violation to client: {}. Marking for removal", 
                    e.getMessage());
                deadEmitters.add(emitter);
            }
        }
        
        violationEmitters.removeAll(deadEmitters);
        log.warn("Violation broadcast completed - Success: {}, Failed: {}, Active emitters: {}", 
            successCount, deadEmitters.size(), violationEmitters.size());
    }
    
    public int getActiveSlotEmitters() {
        return slotEmitters.size();
    }
    
    public int getActiveViolationEmitters() {
        return violationEmitters.size();
    }
}