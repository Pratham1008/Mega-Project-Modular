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
import com.megaproject.notification.service.EmailService;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkImportService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private final SecureRandom secureRandom = new SecureRandom();

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static final int COL_SR_NO     = 0;
    private static final int COL_PRN       = 1;
    private static final int COL_NAME      = 2;
    private static final int COL_GENDER    = 3;
    private static final int COL_DOB       = 4;
    private static final int COL_EMAIL     = 5;
    private static final int COL_PHONE     = 6;
    private static final int COL_ADDRESS   = 7;
    private static final int COL_PINCODE   = 8;
    private static final int COL_CITY      = 9;
    private static final int COL_DISTRICT  = 10;
    private static final int COL_STATE     = 11;
    private static final int COL_BRANCH    = 29;
    private static final int COL_YEAR_DOWN = 38;

    public ImportResult importFromExcel(MultipartFile file) {
        ImportResult result = new ImportResult();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int currentYear = LocalDate.now().getYear();

            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                boolean isEmpty = true;
                for (int c = 0; c < 10; c++) {
                    if (row.getCell(c) != null && row.getCell(c).getCellType() != CellType.BLANK) {
                        isEmpty = false;
                        break;
                    }
                }
                if (isEmpty) continue;

                try {
                    String prn      = getCellString(row, COL_PRN);
                    String fullName = getCellString(row, COL_NAME);
                    String gender   = getCellString(row, COL_GENDER);
                    String dob      = getCellDate(row, COL_DOB);
                    String email    = getCellString(row, COL_EMAIL);
                    String phone    = getCellString(row, COL_PHONE);
                    String address  = getCellString(row, COL_ADDRESS);
                    String pinCode  = getCellString(row, COL_PINCODE);
                    String city     = getCellString(row, COL_CITY);
                    String district = getCellString(row, COL_DISTRICT);
                    String state    = getCellString(row, COL_STATE);
                    String branch   = getCellString(row, COL_BRANCH);

                    if (prn == null || prn.isBlank() || email == null || email.isBlank()
                            || fullName == null || fullName.isBlank()) {
                        result.skipped++;
                        result.addError(i + 1, "Skipped — missing PRN, email, or name");
                        continue;
                    }

                    email = email.trim().toLowerCase();

                    int admissionYear = deriveAdmissionYear(prn);
                    int yearsYearDown = getCellInt(row, COL_YEAR_DOWN);
                    int passingYear   = admissionYear + 4 + yearsYearDown;

                    ProfileType profileType;
                    Role userRole;
                    if (passingYear < currentYear) {
                        profileType = ProfileType.ALUMNI;
                        userRole    = Role.ALUMNI;
                    } else {
                        profileType = ProfileType.STUDENT;
                        userRole    = Role.STUDENT;
                    }

                    String location        = buildLocation(city, state);

                    // Check if user with this email already exists → update instead of skip
                    Optional<User> existingUser = userRepository.findByEmail(email);
                    if (existingUser.isPresent()) {
                        User user = existingUser.get();
                        user.setRole(userRole);
                        userRepository.save(user);

                        Optional<ProfileDocument> existingProfile = profileRepository.findByUserId(user.getId());
                        if (existingProfile.isPresent()) {
                            ProfileDocument profile = existingProfile.get();
                            profile.setFullName(cleanName(fullName));
                            profile.setPhone(cleanPhone(phone));
                            profile.setGender(gender != null ? gender.trim() : profile.getGender());
                            profile.setDateOfBirth(dob != null ? dob : profile.getDateOfBirth());
                            profile.setDepartment(branch != null && !branch.isBlank() ? branch.trim() : profile.getDepartment());
                            profile.setRegistrationNumber(prn.trim());
                            profile.setAdmissionYear(admissionYear);
                            profile.setPassingYear(passingYear);
                            profile.setProfileType(profileType);
                            profile.setLocation(location);
                            if (address != null || city != null || state != null || pinCode != null) {
                                profile.setAddress(Address.builder()
                                        .street(address != null ? address.trim() : (profile.getAddress() != null ? profile.getAddress().getStreet() : null))
                                        .city(city != null ? city.trim() : (profile.getAddress() != null ? profile.getAddress().getCity() : null))
                                        .state(state != null ? state.trim() : (profile.getAddress() != null ? profile.getAddress().getState() : null))
                                        .postalCode(pinCode != null ? pinCode.trim() : (profile.getAddress() != null ? profile.getAddress().getPostalCode() : null))
                                        .country("India")
                                        .build());
                            }
                            profileRepository.save(profile);
                            result.updated++;
                            continue;
                        }
                    }

                    // Check if PRN already exists → update profile
                    Optional<ProfileDocument> existingByPrn = profileRepository.findByRegistrationNumber(prn.trim());
                    if (existingByPrn.isPresent()) {
                        ProfileDocument profile = existingByPrn.get();
                        profile.setFullName(cleanName(fullName));
                        profile.setPhone(cleanPhone(phone));
                        profile.setGender(gender != null ? gender.trim() : profile.getGender());
                        profile.setDateOfBirth(dob != null ? dob : profile.getDateOfBirth());
                        profile.setDepartment(branch != null && !branch.isBlank() ? branch.trim() : profile.getDepartment());
                        profile.setAdmissionYear(admissionYear);
                        profile.setPassingYear(passingYear);
                        profile.setProfileType(profileType);
                        profile.setLocation(location);
                        profileRepository.save(profile);

                        // Also update user role
                        userRepository.findById(profile.getUserId()).ifPresent(u -> {
                            u.setRole(userRole);
                            userRepository.save(u);
                        });

                        result.updated++;
                        continue;
                    }

                    // New user → create
                    String generatedPassword = generateRandomPassword();

                    User user = User.builder()
                            .email(email)
                            .password(passwordEncoder.encode(generatedPassword))
                            .role(userRole)
                            .verified(true)
                            .build();
                    User savedUser = userRepository.save(user);

                    emailService.sendCredentialsEmail(email, cleanName(fullName), generatedPassword);
                    result.credentials.add(new CredentialEntry(email, generatedPassword));

                    ProfileDocument profile = ProfileDocument.builder()
                            .userId(savedUser.getId())
                            .email(email)
                            .fullName(cleanName(fullName))
                            .phone(cleanPhone(phone))
                            .gender(gender != null ? gender.trim() : null)
                            .dateOfBirth(dob)
                            .department(branch != null && !branch.isBlank() ? branch.trim() : "CSE")
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

    private int deriveAdmissionYear(String prn) {
        try {
            String prefix = prn.trim().substring(0, 2);
            int yearShort = Integer.parseInt(prefix);
            return 2000 + yearShort;
        } catch (Exception e) {
            return LocalDate.now().getYear() - 4;
        }
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue(); }
                catch (Exception e) {
                    try { yield String.valueOf((long) cell.getNumericCellValue()); }
                    catch (Exception e2) { yield null; }
                }
            }
            default -> null;
        };
    }

    private int getCellInt(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return 0;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            }
            String s = getCellString(row, col);
            if (s == null || s.isBlank() || s.equals("-")) return 0;
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private String getCellDate(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                java.util.Date date = cell.getDateCellValue();
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
        return name.trim();
    }

    private String cleanPhone(String phone) {
        if (phone == null) return null;
        return phone.trim().replaceAll("[^0-9+]", "");
    }

    private String buildLocation(String city, String state) {
        List<String> parts = new ArrayList<>();
        if (city  != null && !city.isBlank())  parts.add(city.trim());
        if (state != null && !state.isBlank()) parts.add(state.trim());
        return parts.isEmpty() ? "Kolhapur, Maharashtra" : String.join(", ", parts);
    }

    @Data
    public static class ImportResult {
        private int imported = 0;
        private int updated  = 0;
        private int skipped  = 0;
        private List<ErrorEntry> errors = new ArrayList<>();
        private List<CredentialEntry> credentials = new ArrayList<>();

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

    @Data
    @AllArgsConstructor
    public static class CredentialEntry {
        private String email;
        private String password;
    }
}
