package com.megaproject.admin.service;

import com.megaproject.auth.model.Role;
import com.megaproject.auth.model.User;
import com.megaproject.auth.repository.UserRepository;
import com.megaproject.profile.model.*;
import com.megaproject.profile.repository.ProfileRepository;
import com.megaproject.notification.service.EmailService;
import com.megaproject.common.util.PasswordGeneratorUtil;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkImportService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private final PasswordGeneratorUtil passwordGenerator;

    private static final int BATCH_SIZE = 200;

    private static final int COL_SR_NO=0,COL_PRN=1,COL_NAME=2,COL_GENDER=3,
            COL_DOB=4,COL_EMAIL=5,COL_PHONE=6,COL_ADDRESS=7,COL_PINCODE=8,
            COL_CITY=9,COL_DISTRICT=10,COL_STATE=11,COL_BRANCH=29,COL_YEAR_DOWN=38;


    public ImportResult importFromExcel(MultipartFile file) {
        ImportResult result = new ImportResult();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int currentYear = LocalDate.now().getYear();

            Set<String> existingEmails = userRepository.findAllEmails()
                    .stream().map(String::toLowerCase).collect(Collectors.toSet());
            Set<String> existingPrns = new HashSet<>(profileRepository.findAllRegistrationNumbers().size());

            List<User> usersToInsert = new ArrayList<>();
            List<ProfileDocument> profilesToInsert = new ArrayList<>();
            List<CredentialEntry> newCredentials = new ArrayList<>();

            List<UpdateEntry> updateQueue = new ArrayList<>();

            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                try {
                    String prn      = getCellString(row, COL_PRN);
                    String fullName = getCellString(row, COL_NAME);
                    String email    = getCellString(row, COL_EMAIL);

                    if (prn == null || prn.isBlank() || email == null
                            || email.isBlank() || fullName == null || fullName.isBlank()) {
                        result.skipped++;
                        result.addError(i + 1, "Skipped — missing PRN, email, or name");
                        continue;
                    }

                    email = email.trim().toLowerCase();
                    String prnTrimmed = prn.trim();

                    String gender   = getCellString(row, COL_GENDER);
                    String dob      = getCellDate(row);
                    String phone    = getCellString(row, COL_PHONE);
                    String address  = getCellString(row, COL_ADDRESS);
                    String pinCode  = getCellString(row, COL_PINCODE);
                    String city     = getCellString(row, COL_CITY);
                    String district = getCellString(row, COL_DISTRICT);
                    String state    = getCellString(row, COL_STATE);
                    String branch   = getCellString(row, COL_BRANCH);

                    int admissionYear  = deriveAdmissionYear(prnTrimmed);
                    int yearsYearDown  = getCellInt(row);
                    int passingYear    = admissionYear + 4 + yearsYearDown;
                    ProfileType pType  = passingYear < currentYear ? ProfileType.ALUMNI : ProfileType.STUDENT;
                    Role role          = passingYear < currentYear ? Role.ALUMNI : Role.STUDENT;
                    String location    = buildLocation(city, state);

                    if (existingEmails.contains(email) || existingPrns.contains(prnTrimmed)) {
                        updateQueue.add(new UpdateEntry(email, prnTrimmed, cleanName(fullName),
                                cleanPhone(phone), gender, dob, branch, admissionYear,
                                passingYear, pType, role, location, address, city, state, pinCode));
                        result.updated++;
                        continue;
                    }

                    String password = passwordGenerator.generate();
                    User user = User.builder()
                            .email(email).password(passwordEncoder.encode(password))
                            .role(role).verified(true).build();
                    usersToInsert.add(user);

                    newCredentials.add(new CredentialEntry(email, password,
                            cleanName(fullName), buildProfile(email, fullName, phone, gender,
                            dob, branch, prnTrimmed, admissionYear, passingYear, pType,
                            location, address, city, state, pinCode)));

                    existingEmails.add(email);
                    existingPrns.add(prnTrimmed);

                    if (usersToInsert.size() >= BATCH_SIZE) {
                        flushBatch(usersToInsert, newCredentials, profilesToInsert, result);
                    }

                } catch (Exception e) {
                    result.addError(i + 1, "Error: " + e.getMessage());
                    log.warn("Import error at row {}: {}", i + 1, e.getMessage());
                }
            }

            if (!usersToInsert.isEmpty()) {
                flushBatch(usersToInsert, newCredentials, profilesToInsert, result);
            }

            processUpdates(updateQueue);

            result.credentials.addAll(newCredentials.stream()
                    .map(c -> new CredentialEntry(c.email, c.password)).toList());

        } catch (Exception e) {
            result.addError(0, "Failed to read Excel file: " + e.getMessage());
            log.error("Excel import failed", e);
        }

        return result;
    }

    private void flushBatch(List<User> users, List<CredentialEntry> creds,
                            List<ProfileDocument> profiles, ImportResult result) {

        List<User> saved = userRepository.saveAll(users);

        for (int i = 0; i < saved.size(); i++) {
            CredentialEntry ce = creds.get(i);
            ProfileDocument pd = ce.profileTemplate;
            pd.setUserId(saved.get(i).getId());
            profiles.add(pd);
        }
        profileRepository.saveAll(profiles);
        result.imported += saved.size();

        List<CredentialEntry> batch = List.copyOf(creds);
        sendEmailsAsync(batch);

        users.clear();
        creds.clear();
        profiles.clear();
    }

    @Async("taskExecutor")
    protected void sendEmailsAsync(List<CredentialEntry> entries) {
        for (CredentialEntry e : entries) {
            try {
                emailService.sendCredentialsEmail(e.email, e.displayName, e.password);
            } catch (Exception ex) {
                log.warn("Failed to send credentials email to {}: {}", e.email, ex.getMessage());
            }
        }
    }

    private void processUpdates(List<UpdateEntry> updates) {
        if (updates.isEmpty()) return;
        Set<String> emails = updates.stream().map(u -> u.email).collect(Collectors.toSet());
        Map<String, User> userMap = userRepository.findAllByEmailIn(emails)
                .stream().collect(Collectors.toMap(u -> u.getEmail().toLowerCase(), u -> u));
        Map<String, ProfileDocument> profileByEmail = profileRepository.findAllByEmailIn(emails)
                .stream().collect(Collectors.toMap(p -> p.getEmail().toLowerCase(), p -> p));

        List<User> usersToSave = new ArrayList<>();
        List<ProfileDocument> profilesToSave = new ArrayList<>();

        for (UpdateEntry u : updates) {
            User user = userMap.get(u.email);
            if (user != null) {
                user.setRole(u.role);
                usersToSave.add(user);
            }
            ProfileDocument profile = profileByEmail.get(u.email);
            if (profile != null) {
                applyUpdate(profile, u);
                profilesToSave.add(profile);
            }
        }
        userRepository.saveAll(usersToSave);
        profileRepository.saveAll(profilesToSave);
    }

    private void applyUpdate(ProfileDocument p, UpdateEntry u) {
        p.setFullName(u.fullName); p.setPhone(u.phone);
        if (u.gender != null) p.setGender(u.gender.trim());
        if (u.dob != null) p.setDateOfBirth(u.dob);
        if (u.branch != null && !u.branch.isBlank()) p.setDepartment(u.branch.trim());
        p.setAdmissionYear(u.admissionYear); p.setPassingYear(u.passingYear);
        p.setProfileType(u.profileType); p.setLocation(u.location);
        p.setAddress(Address.builder()
                .street(u.address != null ? u.address.trim() : null)
                .city(u.city != null ? u.city.trim() : null)
                .state(u.state != null ? u.state.trim() : null)
                .postalCode(u.pinCode != null ? u.pinCode.trim() : null)
                .country("India").build());
    }

    private ProfileDocument buildProfile(String email, String fullName,
                                         String phone, String gender, String dob, String branch, String prn,
                                         int admYear, int passYear, ProfileType pType, String location,
                                         String address, String city, String state, String pinCode) {
        return ProfileDocument.builder()
                .userId(null).email(email)
                .fullName(cleanName(fullName)).phone(cleanPhone(phone))
                .gender(gender != null ? gender.trim() : null).dateOfBirth(dob)
                .department(branch != null && !branch.isBlank() ? branch.trim() : "CSE")
                .registrationNumber(prn).admissionYear(admYear).passingYear(passYear)
                .profileType(pType).location(location).approved(true).deleted(false)
                .address(Address.builder()
                        .street(address != null ? address.trim() : null)
                        .city(city != null ? city.trim() : null)
                        .state(state != null ? state.trim() : null)
                        .postalCode(pinCode != null ? pinCode.trim() : null)
                        .country("India").build())
                .build();
    }

    // ── Cell helpers (unchanged) ───────────────────────────────────────────

    private boolean isRowEmpty(Row row) {
        for (int c = 0; c < 10; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    private int deriveAdmissionYear(String prn) {
        try { return 2000 + Integer.parseInt(prn.substring(0, 2)); }
        catch (Exception e) { return LocalDate.now().getYear() - 4; }
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                yield v == Math.floor(v) && !Double.isInfinite(v)
                        ? String.valueOf((long) v) : String.valueOf(v);
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

    private int getCellInt(Row row) {
        Cell cell = row.getCell(BulkImportService.COL_YEAR_DOWN);
        if (cell == null) return 0;
        try {
            if (cell.getCellType() == CellType.NUMERIC) return (int) cell.getNumericCellValue();
            String s = getCellString(row, BulkImportService.COL_YEAR_DOWN);
            if (s == null || s.isBlank() || s.equals("-")) return 0;
            return Integer.parseInt(s.trim());
        } catch (Exception e) { return 0; }
    }

    private String getCellDate(Row row) {
        Cell cell = row.getCell(BulkImportService.COL_DOB);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                java.util.Date date = cell.getDateCellValue();
                return Instant.ofEpochMilli(date.getTime())
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
            return getCellString(row, BulkImportService.COL_DOB);
        } catch (Exception e) { return null; }
    }

    private String cleanName(String name) { return name == null ? null : name.trim(); }
    private String cleanPhone(String p) { return p == null ? null : p.trim().replaceAll("[^0-9+]", ""); }
    private String buildLocation(String city, String state) {
        List<String> parts = new ArrayList<>();
        if (city != null && !city.isBlank()) parts.add(city.trim());
        if (state != null && !state.isBlank()) parts.add(state.trim());
        return parts.isEmpty() ? "Kolhapur, Maharashtra" : String.join(", ", parts);
    }

    @Data
    public static class ImportResult {
        int imported=0, updated=0, skipped=0;
        List<ErrorEntry> errors = new ArrayList<>();
        List<CredentialEntry> credentials = new ArrayList<>();
        public void addError(int row, String msg) { errors.add(new ErrorEntry(row, msg)); }
    }

    @Data @AllArgsConstructor
    public static class ErrorEntry { int row; String message; }

    @Data @AllArgsConstructor
    public static class CredentialEntry {
        String email; String password;
        // Extra fields used only during processing, not serialised in response
        transient String displayName;
        transient ProfileDocument profileTemplate;
        public CredentialEntry(String email, String password) { this(email, password, null, null); }
    }

    private record UpdateEntry(String email, String prn, String fullName, String phone, String gender, String dob,
                               String branch, int admissionYear, int passingYear, ProfileType profileType, Role role,
                               String location, String address, String city, String state, String pinCode) {
    }
}