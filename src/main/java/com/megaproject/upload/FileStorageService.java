package com.megaproject.upload;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.base-url}")
    private String baseUrl;

    private Path profilePhotosPath;
    private Path eventBannersPath;

    @PostConstruct
    public void init() {
        try {
            profilePhotosPath = Paths.get(uploadDir, "profile-photos").toAbsolutePath();
            eventBannersPath  = Paths.get(uploadDir, "event-banners").toAbsolutePath();
            Files.createDirectories(profilePhotosPath);
            Files.createDirectories(eventBannersPath);
            log.info("Upload directories initialised: {}, {}", profilePhotosPath, eventBannersPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directories", e);
        }
    }

    public String storeProfilePhoto(MultipartFile file) {
        validateImage(file);
        String filename = "profile_" + UUID.randomUUID() + getExtension(file);
        store(file, profilePhotosPath.resolve(filename));
        return baseUrl + "/uploads/profile-photos/" + filename;
    }

    public String storeEventBanner(MultipartFile file) {
        validateImage(file);
        String filename = "banner_" + UUID.randomUUID() + getExtension(file);
        store(file, eventBannersPath.resolve(filename));
        return baseUrl + "/uploads/event-banners/" + filename;
    }

    public void deleteFile(String filename) {
        // Prevent path traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("Invalid filename: " + filename);
        }
        // Try both subdirectories
        tryDelete(profilePhotosPath.resolve(filename));
        tryDelete(eventBannersPath.resolve(filename));
    }

    // ── Private helpers ─────────────────────────────────────────────────────────

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File exceeds maximum size of 5 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type. Allowed: JPEG, PNG, WebP, GIF");
        }
    }

    private void store(MultipartFile file, Path destination) {
        try {
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + destination.getFileName(), e);
        }
    }

    private void tryDelete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Could not delete file {}: {}", path, e.getMessage());
        }
    }

    private String getExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            return original.substring(original.lastIndexOf('.'));
        }
        String ct = file.getContentType();
        if (ct != null) {
            return switch (ct.toLowerCase()) {
                case "image/jpeg", "image/jpg" -> ".jpg";
                case "image/png"               -> ".png";
                case "image/webp"              -> ".webp";
                case "image/gif"               -> ".gif";
                default                        -> ".bin";
            };
        }
        return ".bin";
    }
}

