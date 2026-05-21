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
import com.megaproject.profile.dto.response.AlumniSearchResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkExportService {

    private final ProfileRepository profileRepository;

        private static final String[] HEADERS = {
            "Sr. No",
            "PRN",
            "Student Name ( Surname Name Middle)",
            "Gender",
            "Date Of Birth (MM/DD/YY)",
            "Email ID",
            "Mobile Number",
            "Full  Address with Pin Code",
            "Pin Code",
            "City as per Domicile Certificate",
            "Home District",
            "State as per Domicile Certificate",
            "Branch",
            "Profile Type",
            "Admission Year",
            "Passing Year",
            "Current Semester",
            "Job Title",
            "Company",
            "Location",
            "Skills",
            "Resume URL",
            "Photo URL",
            "Blood Group",
            "LinkedIn URL",
            "GitHub URL",
            "Instagram URL",
            "Approved"
    };

    public byte[] exportToExcel() throws IOException {
        List<ProfileDocument> profiles = profileRepository.findByDeletedFalse();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("CSE");

            
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("KIT's College of Engineering Kolhapur (Empowered Autonomous)");
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            
            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            
            int rowIdx = 2;
            int serial = 1;
            for (ProfileDocument p : profiles) {
                Row row = sheet.createRow(rowIdx++);
                int col = 0;

                row.createCell(col++).setCellValue(serial++);                               
                row.createCell(col++).setCellValue(safe(p.getRegistrationNumber()));         
                row.createCell(col++).setCellValue(safe(p.getFullName()));                   
                row.createCell(col++).setCellValue(safe(p.getGender()));                     
                row.createCell(col++).setCellValue(safe(p.getDateOfBirth()));                

                row.createCell(col++).setCellValue(safe(p.getEmail()));                      
                row.createCell(col++).setCellValue(safe(p.getPhone()));                      

                
                String street = "", pinCode = "", city = "", district = "", state = "";
                if (p.getAddress() != null) {
                    street = safe(p.getAddress().getStreet());
                    pinCode = safe(p.getAddress().getPostalCode());
                    city = safe(p.getAddress().getCity());
                    state = safe(p.getAddress().getState());
                    district = city; // district not stored separately, use city
                }
                row.createCell(col++).setCellValue(street);                                  // Full Address with Pin Code
                row.createCell(col++).setCellValue(pinCode);                                 // Pin Code
                row.createCell(col++).setCellValue(city);                                    // City as per Domicile Certificate
                row.createCell(col++).setCellValue(district);                                // Home District
                row.createCell(col++).setCellValue(state);                                   // State as per Domicile Certificate

                row.createCell(col++).setCellValue(safe(p.getDepartment()));                  // Branch
                row.createCell(col++).setCellValue(
                        p.getProfileType() != null ? p.getProfileType().name() : "");        // Profile Type
                row.createCell(col++).setCellValue(
                        p.getAdmissionYear() != null ? p.getAdmissionYear() : 0);            // Admission Year
                row.createCell(col++).setCellValue(
                        p.getPassingYear() != null ? p.getPassingYear() : 0);                // Passing Year
                row.createCell(col++).setCellValue(
                        p.getCurrentSemester() != null ? p.getCurrentSemester() : 0);        // Current Semester

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
                row.createCell(col++).setCellValue(linkedin);                                // LinkedIn URL
                row.createCell(col++).setCellValue(github);                                  // GitHub URL
                row.createCell(col++).setCellValue(instagram);                               // Instagram URL

                row.createCell(col).setCellValue(p.isApproved() ? "Yes" : "No");            
            }

            
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private String safe(String val) {
        return val != null ? val : "";
    }

    /**
     * Export a filtered set of profiles (from search results) to Excel.
     */
    public byte[] exportFilteredToExcel(List<AlumniSearchResponse> results) throws IOException {
        // Look up full ProfileDocuments for the filtered user IDs
        List<String> userIds = results.stream().map(AlumniSearchResponse::getUserId).toList();
        List<ProfileDocument> profiles = profileRepository.findAllByUserIdIn(userIds);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Filtered Export");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("KIT's College of Engineering — Filtered Export (" + profiles.size() + " records)");
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 2;
            int serial = 1;
            for (ProfileDocument p : profiles) {
                Row row = sheet.createRow(rowIdx++);
                int col = 0;
                row.createCell(col++).setCellValue(serial++);
                row.createCell(col++).setCellValue(safe(p.getRegistrationNumber()));
                row.createCell(col++).setCellValue(safe(p.getFullName()));
                row.createCell(col++).setCellValue(safe(p.getGender()));
                row.createCell(col++).setCellValue(safe(p.getDateOfBirth()));
                row.createCell(col++).setCellValue(safe(p.getEmail()));
                row.createCell(col++).setCellValue(safe(p.getPhone()));
                String street = "", pinCode = "", city = "", district = "", state = "";
                if (p.getAddress() != null) {
                    street = safe(p.getAddress().getStreet());
                    pinCode = safe(p.getAddress().getPostalCode());
                    city = safe(p.getAddress().getCity());
                    state = safe(p.getAddress().getState());
                    district = city;
                }
                row.createCell(col++).setCellValue(street);
                row.createCell(col++).setCellValue(pinCode);
                row.createCell(col++).setCellValue(city);
                row.createCell(col++).setCellValue(district);
                row.createCell(col++).setCellValue(state);
                row.createCell(col++).setCellValue(safe(p.getDepartment()));
                row.createCell(col++).setCellValue(p.getProfileType() != null ? p.getProfileType().name() : "");
                row.createCell(col++).setCellValue(p.getAdmissionYear() != null ? p.getAdmissionYear() : 0);
                row.createCell(col++).setCellValue(p.getPassingYear() != null ? p.getPassingYear() : 0);
                row.createCell(col++).setCellValue(p.getCurrentSemester() != null ? p.getCurrentSemester() : 0);
                row.createCell(col++).setCellValue(safe(p.getJobTitle()));
                row.createCell(col++).setCellValue(safe(p.getCompany()));
                row.createCell(col++).setCellValue(safe(p.getLocation()));
                String skills = (p.getSkills() != null && !p.getSkills().isEmpty()) ? String.join(", ", p.getSkills()) : "";
                row.createCell(col++).setCellValue(skills);
                row.createCell(col++).setCellValue(safe(p.getResumeUrl()));
                row.createCell(col++).setCellValue(safe(p.getPhotoUrl()));
                row.createCell(col++).setCellValue(safe(p.getBloodGroup()));
                String linkedin = "", github = "", instagram = "";
                if (p.getSocials() != null) {
                    linkedin = safe(p.getSocials().getLinkedinUrl());
                    github = safe(p.getSocials().getGithubUrl());
                    instagram = safe(p.getSocials().getInstagramUrl());
                }
                row.createCell(col++).setCellValue(linkedin);
                row.createCell(col++).setCellValue(github);
                row.createCell(col++).setCellValue(instagram);
                row.createCell(col).setCellValue(p.isApproved() ? "Yes" : "No");
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
