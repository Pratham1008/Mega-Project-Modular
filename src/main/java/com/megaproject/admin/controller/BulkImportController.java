package com.megaproject.admin.controller;

import com.megaproject.admin.service.BulkImportService;
import com.megaproject.admin.service.BulkImportService.ImportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Admin-only endpoint for bulk importing student/alumni data from Excel files.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class BulkImportController {

    private final BulkImportService importService;

    /**
     * Import student/alumni profiles from an Excel (.xlsx) file.
     * Auto-creates auth accounts with default password KIT@{PRN}.
     *
     * Smart role detection:
     *   - passingYear <= current year → ALUMNI
     *   - passingYear > current year  → STUDENT
     */
    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importExcel(
            @RequestParam("file") MultipartFile file) {

        // Validate file type
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Only .xlsx files are accepted"));
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "File is empty"));
        }

        ImportResult result = importService.importFromExcel(file);

        return ResponseEntity.ok(result);
    }
}
