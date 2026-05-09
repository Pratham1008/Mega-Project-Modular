package com.megaproject.admin.service;

import com.megaproject.profile.model.ProfileDocument;
import com.megaproject.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkExportService {

    private final ProfileRepository profileRepository;

    // Column headers matching the import format
    private static final String[] HEADERS = {
            "Sr. No.", "PRN", "Full Name", "Gender", "Date of Birth",
            "Email", "Phone", "Address", "Pin Code", "City",
            "District", "State", "Profile Type", "Admission Year",
            "Passing Year", "Current Semester", "Department",
            "Job Title", "Company", "Location", "Skills",
            "Resume URL", "Photo URL", "Blood Group",
            "LinkedIn URL", "GitHub URL", "Instagram URL", "Approved"
    };

    public byte[] exportToExcel() throws IOException {
        List<ProfileDocument> profiles = profileRepository.findByDeletedFalse();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Profiles");

            // ── Header style ──
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            // ── Title row (Row 0) ──
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Alumni Connect — Profile Export");
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            // ── Column headers (Row 1) ──
            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // ── Data rows (Row 2+) ──
            int rowIdx = 2;
            int serial = 1;
            for (ProfileDocument p : profiles) {
                Row row = sheet.createRow(rowIdx++);
                int col = 0;

                row.createCell(col++).setCellValue(serial++);                               // Sr. No.
                row.createCell(col++).setCellValue(safe(p.getRegistrationNumber()));         // PRN
                row.createCell(col++).setCellValue(safe(p.getFullName()));                   // Full Name
                row.createCell(col++).setCellValue("");                                      // Gender (not stored)
                row.createCell(col++).setCellValue(safe(p.getDateOfBirth()));                // DOB

                row.createCell(col++).setCellValue(safe(p.getEmail()));                      // Email
                row.createCell(col++).setCellValue(safe(p.getPhone()));                      // Phone

                // Address fields
                String street = "", pinCode = "", city = "", district = "", state = "";
                if (p.getAddress() != null) {
                    street = safe(p.getAddress().getStreet());
                    pinCode = safe(p.getAddress().getPostalCode());
                    city = safe(p.getAddress().getCity());
                    state = safe(p.getAddress().getState());
                    district = city; // district not stored separately, use city
                }
                row.createCell(col++).setCellValue(street);                                  // Address
                row.createCell(col++).setCellValue(pinCode);                                 // Pin Code
                row.createCell(col++).setCellValue(city);                                    // City
                row.createCell(col++).setCellValue(district);                                // District
                row.createCell(col++).setCellValue(state);                                   // State

                row.createCell(col++).setCellValue(
                        p.getProfileType() != null ? p.getProfileType().name() : "");        // Profile Type
                row.createCell(col++).setCellValue(
                        p.getAdmissionYear() != null ? p.getAdmissionYear() : 0);            // Admission Year
                row.createCell(col++).setCellValue(
                        p.getPassingYear() != null ? p.getPassingYear() : 0);                // Passing Year
                row.createCell(col++).setCellValue(
                        p.getCurrentSemester() != null ? p.getCurrentSemester() : 0);        // Current Semester
                row.createCell(col++).setCellValue(safe(p.getDepartment()));                  // Department

                row.createCell(col++).setCellValue(safe(p.getJobTitle()));                   // Job Title
                row.createCell(col++).setCellValue(safe(p.getCompany()));                    // Company
                row.createCell(col++).setCellValue(safe(p.getLocation()));                   // Location

                // Skills as comma-separated
                String skills = (p.getSkills() != null && !p.getSkills().isEmpty())
                        ? String.join(", ", p.getSkills()) : "";
                row.createCell(col++).setCellValue(skills);                                  // Skills

                row.createCell(col++).setCellValue(safe(p.getResumeUrl()));                  // Resume URL
                row.createCell(col++).setCellValue(safe(p.getPhotoUrl()));                   // Photo URL
                row.createCell(col++).setCellValue(safe(p.getBloodGroup()));                 // Blood Group

                // Socials
                String linkedin = "", github = "", instagram = "";
                if (p.getSocials() != null) {
                    linkedin = safe(p.getSocials().getLinkedinUrl());
                    github = safe(p.getSocials().getGithubUrl());
                    instagram = safe(p.getSocials().getInstagramUrl());
                }
                row.createCell(col++).setCellValue(linkedin);                                // LinkedIn
                row.createCell(col++).setCellValue(github);                                  // GitHub
                row.createCell(col++).setCellValue(instagram);                               // Instagram

                row.createCell(col).setCellValue(p.isApproved() ? "Yes" : "No");            // Approved
            }

            // Auto-size columns
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            log.info("Exported {} profiles to Excel", profiles.size());
            return out.toByteArray();
        }
    }

    private String safe(String val) {
        return val != null ? val : "";
    }
}
