package com.megaproject.upload;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @PostMapping("/profile-photo")
    public ResponseEntity<Map<String, String>> uploadProfilePhoto(
            @RequestParam("file") MultipartFile file) {
        String url = fileStorageService.storeProfilePhoto(file);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping("/event-banner")
    public ResponseEntity<Map<String, String>> uploadEventBanner(
            @RequestParam("file") MultipartFile file) {
        String url = fileStorageService.storeEventBanner(file);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @DeleteMapping("/{filename:.+}")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable String filename) {
        fileStorageService.deleteFile(filename);
        return ResponseEntity.ok(Map.of("success", true, "message", "File deleted"));
    }
}

