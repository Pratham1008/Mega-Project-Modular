package com.megaproject.admin.controller;

import com.megaproject.admin.service.BulkExportService;
import com.megaproject.admin.service.BulkImportService;
import com.megaproject.admin.service.BulkRoleService;
import com.megaproject.admin.service.BulkRoleService.BulkRoleResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class BulkImportController {

    private final BulkImportService importService;
    private final BulkExportService exportService;
    private final BulkRoleService   roleService;

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
    @PreAuthorize("hasRole('ADMIN')")
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

    @PostMapping("/bulk-role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BulkRoleResult> bulkChangeRole(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> userIds = (List<String>) body.get("userIds");
        String role          = (String) body.get("role");

        if (userIds == null || userIds.isEmpty() || role == null || role.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(roleService.changeRoles(userIds, role));
    }
}
