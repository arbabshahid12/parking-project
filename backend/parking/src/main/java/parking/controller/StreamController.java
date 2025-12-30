package parking.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import parking.services.SSEService;

@RestController
@RequestMapping("/stream")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class StreamController {
    
    private final SSEService sseService;
    
    @GetMapping(value = "/slots", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSlotUpdates() {
        log.info("=== New SSE connection request for slot updates ===");
        return sseService.createSlotEmitter();
    }
    
    @GetMapping(value = "/violations", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamViolations() {
        log.info("=== New SSE connection request for violations ===");
        return sseService.createViolationEmitter();
    }
    
    @GetMapping("/status")
    public ResponseEntity<String> getStreamStatus() {
        int slotEmitters = sseService.getActiveSlotEmitters();
        int violationEmitters = sseService.getActiveViolationEmitters();
        String status = String.format(
            "Active slot streams: %d, Active violation streams: %d",
            slotEmitters, violationEmitters
        );
        log.info("Stream status: {}", status);
        return ResponseEntity.ok(status);
    }
}