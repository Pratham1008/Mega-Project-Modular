package com.megaproject.admin.controller;

import com.megaproject.admin.service.BulkExportService;
import com.megaproject.admin.service.BulkImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import com.megaproject.profile.service.AlumniSearchService;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class BulkImportController {

    private final BulkImportService importService;
    private final BulkExportService exportService;
    private final AlumniSearchService alumniSearchService;

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importExcel(@RequestParam("file") MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only .xlsx files are accepted"));
        }
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }
        return ResponseEntity.ok(importService.importFromExcel(file));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ResponseEntity<byte[]> exportExcel() {
        try {
            byte[] data = exportService.exportToExcel();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("alumni_connect_export.xlsx").build());
            headers.setContentLength(data.length);
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/filtered")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ResponseEntity<byte[]> exportFilteredExcel(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) Integer passingYear,
            @RequestParam(required = false) String location) {
        try {
            var results = alumniSearchService.searchWithFilters(q, department, company, passingYear, location);
            byte[] data = exportService.exportFilteredToExcel(results);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("alumni_connect_filtered_export.xlsx").build());
            headers.setContentLength(data.length);
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}
