package com.megaproject.admin.service;

import com.megaproject.auth.model.Role;
import com.megaproject.auth.model.User;
import com.megaproject.auth.repository.UserRepository;
import com.megaproject.profile.model.*;
import com.megaproject.profile.repository.ProfileRepository;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkImportService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Import student/alumni data from an Excel (.xlsx) file.
     *
     * Excel structure (from Master Student Database):
     *   Row 1: College header (skip)
     *   Row 2: Column headers (PRN, Student Name, Gender, DOB, Email, ...)
     *   Row 3+: Data
     *
     * For each row:
     *   1. Auto-create auth User with default password KIT@{PRN}
     *   2. Create ProfileDocument
     *   3. Smart role detection: if passingYear <= currentYear → ALUMNI, else STUDENT
     */
    public ImportResult importFromExcel(MultipartFile file) {
        ImportResult result = new ImportResult();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int currentYear = LocalDate.now().getYear();

            // Data starts at row index 2 (0-based), row 0 = college header, row 1 = column headers
            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String prn = getCellString(row, 1);       // Col B: PRN
                    String fullName = getCellString(row, 2);   // Col C: Student Name
                    String gender = getCellString(row, 3);     // Col D: Gender
                    String dob = getCellDate(row, 4);          // Col E: Date of Birth
                    String email = getCellString(row, 5);      // Col F: Email
                    String phone = getCellString(row, 6);      // Col G: Mobile
                    String address = getCellString(row, 7);    // Col H: Full Address
                    String pinCode = getCellString(row, 8);    // Col I: Pin Code
                    String city = getCellString(row, 9);       // Col J: City
                    String district = getCellString(row, 10);  // Col K: District
                    String state = getCellString(row, 11);     // Col L: State
                    String branch = getCellString(row, 29);    // Col AD (index 29): Branch

                    // ── Validate essential fields ──
                    if (prn == null || prn.isBlank() || email == null || email.isBlank()
                            || fullName == null || fullName.isBlank()) {
                        result.addError(i + 1, "Missing PRN, email, or name");
                        continue;
                    }

                    email = email.trim().toLowerCase();

                    // ── Skip if email already exists ──
                    if (userRepository.existsByEmail(email)) {
                        result.skipped++;
                        result.addError(i + 1, "Email already exists: " + email);
                        continue;
                    }

                    // ── Skip if PRN already exists ──
                    if (profileRepository.existsByRegistrationNumber(prn.trim())) {
                        result.skipped++;
                        result.addError(i + 1, "PRN already exists: " + prn);
                        continue;
                    }

                    // ── Derive admission & passing year from PRN ──
                    int admissionYear = deriveAdmissionYear(prn);
                    int passingYear = admissionYear + 4;

                    // ── Smart role detection ──
                    ProfileType profileType;
                    Role userRole;
                    if (passingYear <= currentYear) {
                        profileType = ProfileType.ALUMNI;
                        userRole = Role.ALUMNI;
                    } else {
                        profileType = ProfileType.STUDENT;
                        userRole = Role.STUDENT;
                    }

                    // ── Build location string ──
                    String location = buildLocation(city, state);

                    // ── 1. Create auth User ──
                    String defaultPassword = "KIT@" + prn.trim();
                    User user = User.builder()
                            .email(email)
                            .password(passwordEncoder.encode(defaultPassword))
                            .role(userRole)
                            .verified(true)  // admin-imported = trusted
                            .build();
                    User savedUser = userRepository.save(user);

                    // ── 2. Create Profile ──
                    ProfileDocument profile = ProfileDocument.builder()
                            .userId(savedUser.getId())
                            .email(email)
                            .fullName(cleanName(fullName))
                            .phone(cleanPhone(phone))
                            .dateOfBirth(dob)
                            .department(branch != null ? branch.trim() : "CSE")
                            .registrationNumber(prn.trim())
                            .admissionYear(admissionYear)
                            .passingYear(passingYear)
                            .profileType(profileType)
                            .location(location)
                            .approved(true)
                            .deleted(false)
                            .address(Address.builder()
                                    .street(address != null ? address.trim() : null)
                                    .city(city != null ? city.trim() : null)
                                    .state(state != null ? state.trim() : null)
                                    .postalCode(pinCode != null ? pinCode.trim() : null)
                                    .country("India")
                                    .build())
                            .build();
                    profileRepository.save(profile);

                    result.imported++;
                    log.info("Imported: {} ({}) as {}", fullName, email, profileType);

                } catch (Exception e) {
                    result.addError(i + 1, "Error: " + e.getMessage());
                    log.warn("Import error at row {}: {}", i + 1, e.getMessage());
                }
            }
        } catch (Exception e) {
            result.addError(0, "Failed to read Excel file: " + e.getMessage());
            log.error("Excel import failed", e);
        }

        return result;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private int deriveAdmissionYear(String prn) {
        // PRN format: YYMM000XXX — first 2 digits = year prefix
        // e.g. 2122000824 → 21 → 2021, 2223000366 → 22 → 2022
        try {
            String prefix = prn.trim().substring(0, 2);
            int yearShort = Integer.parseInt(prefix);
            return 2000 + yearShort;
        } catch (Exception e) {
            return LocalDate.now().getYear() - 3; // fallback
        }
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                // If it's a whole number, strip the decimal
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue(); }
                catch (Exception e) {
                    try { yield String.valueOf(cell.getNumericCellValue()); }
                    catch (Exception e2) { yield null; }
                }
            }
            default -> null;
        };
    }

    private String getCellDate(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                Date date = cell.getDateCellValue();
                LocalDate ld = Instant.ofEpochMilli(date.getTime())
                        .atZone(ZoneId.systemDefault()).toLocalDate();
                return ld.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
            return getCellString(row, col);
        } catch (Exception e) {
            return null;
        }
    }

    private String cleanName(String name) {
        if (name == null) return null;
        // "Surname FirstName Middle" → keep as-is (natural format in India)
        return name.trim();
    }

    private String cleanPhone(String phone) {
        if (phone == null) return null;
        return phone.trim().replaceAll("[^0-9+]", "");
    }

    private String buildLocation(String city, String state) {
        List<String> parts = new ArrayList<>();
        if (city != null && !city.isBlank()) parts.add(city.trim());
        if (state != null && !state.isBlank()) parts.add(state.trim());
        return parts.isEmpty() ? "Kolhapur, Maharashtra" : String.join(", ", parts);
    }

    // ── Result DTO ──────────────────────────────────────────────────────────

    @Data
    public static class ImportResult {
        private int imported = 0;
        private int skipped = 0;
        private List<ErrorEntry> errors = new ArrayList<>();

        public void addError(int row, String message) {
            errors.add(new ErrorEntry(row, message));
        }
    }

    @Data
    @AllArgsConstructor
    public static class ErrorEntry {
        private int row;
        private String message;
    }
}
