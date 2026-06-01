package com.megaproject.admin.service;

import com.megaproject.profile.dto.response.AlumniSearchResponse;
import com.megaproject.profile.model.ProfileDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;   // ← streaming API; low memory
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkExportService {

    private final MongoTemplate mongoTemplate;

    private static final String[] HEADERS = {
            "Sr. No","PRN","Student Name","Gender","Date Of Birth","Email ID","Mobile Number",
            "Full Address","Pin Code","City","District","State","Branch","Profile Type",
            "Admission Year","Passing Year","Job Title","Company","Location","Skills",
            "Resume URL","Photo URL","Blood Group","LinkedIn URL","GitHub URL","Instagram URL","Approved"
    };

    private static final String[] FILTERED_HEADERS = {
            "Sr. No","Name","Email","Profile Type","Job Title","Company",
            "Location","Department","Passing Year","Skills"
    };

    public byte[] exportToExcel() throws IOException {

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("KIT");

            CellStyle headerStyle = buildHeaderStyle(workbook);
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue(
                    "KIT's College of Engineering Kolhapur (Empowered Autonomous)");

            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(HEADERS[i]);
                c.setCellStyle(headerStyle);
            }

            Query q = new Query(Criteria.where("deleted").is(false));
            q.fields()
                    .include("registrationNumber","fullName","gender","dateOfBirth","email",
                            "phone","address","department","profileType","admissionYear",
                            "passingYear","jobTitle","company","location","skills",
                            "resumeUrl","photoUrl","bloodGroup","socials","approved");

            try (var data = mongoTemplate.stream(q, ProfileDocument.class)) {
                AtomicInteger rowNum = new AtomicInteger(2);
                AtomicInteger srNo   = new AtomicInteger(1);

                Iterator<ProfileDocument> cursor = data.iterator();
                while (cursor.hasNext()) {
                    ProfileDocument p = cursor.next();
                    Row row = sheet.createRow(rowNum.getAndIncrement());
                    writeProfileRow(row, srNo.getAndIncrement(), p);
                }

            }

            workbook.write(out);
            workbook.close();
            return out.toByteArray();
        }
    }

    /**
     * Export a pre-filtered list of search results to Excel.
     * Uses a slimmer column set since search results don't carry all profile fields.
     */
    public byte[] exportFilteredToExcel(List<AlumniSearchResponse> results) throws IOException {

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Filtered Alumni");

            CellStyle headerStyle = buildHeaderStyle(workbook);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < FILTERED_HEADERS.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(FILTERED_HEADERS[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            int srNo = 1;
            for (AlumniSearchResponse r : results) {
                Row row = sheet.createRow(rowIdx++);
                int col = 0;
                row.createCell(col++).setCellValue(srNo++);
                row.createCell(col++).setCellValue(str(r.getFullName()));
                row.createCell(col++).setCellValue(str(r.getEmail()));
                row.createCell(col++).setCellValue(str(r.getProfileType()));
                row.createCell(col++).setCellValue(str(r.getJobTitle()));
                row.createCell(col++).setCellValue(str(r.getCompany()));
                row.createCell(col++).setCellValue(str(r.getLocation()));
                row.createCell(col++).setCellValue(str(r.getDepartment()));
                row.createCell(col++).setCellValue(r.getPassingYear() != null ? r.getPassingYear() : 0);
                row.createCell(col).setCellValue(
                        r.getSkills() != null ? String.join(", ", r.getSkills()) : "");
            }

            workbook.write(out);
            workbook.close();
            return out.toByteArray();
        }
    }

    private void writeProfileRow(Row row, int srNo, ProfileDocument p) {
        int col = 0;
        row.createCell(col++).setCellValue(srNo);
        row.createCell(col++).setCellValue(str(p.getRegistrationNumber()));
        row.createCell(col++).setCellValue(str(p.getFullName()));
        row.createCell(col++).setCellValue(str(p.getGender()));
        row.createCell(col++).setCellValue(str(p.getDateOfBirth()));
        row.createCell(col++).setCellValue(str(p.getEmail()));
        row.createCell(col++).setCellValue(str(p.getPhone()));
        var addr = p.getAddress();
        row.createCell(col++).setCellValue(addr != null ? str(addr.getStreet()) : "");
        row.createCell(col++).setCellValue(addr != null ? str(addr.getPostalCode()) : "");
        row.createCell(col++).setCellValue(addr != null ? str(addr.getCity()) : "");
        row.createCell(col++).setCellValue(""); // district not stored separately
        row.createCell(col++).setCellValue(addr != null ? str(addr.getState()) : "");
        row.createCell(col++).setCellValue(str(p.getDepartment()));
        row.createCell(col++).setCellValue(p.getProfileType() != null ? p.getProfileType().name() : "");
        row.createCell(col++).setCellValue(p.getAdmissionYear());
        row.createCell(col++).setCellValue(p.getPassingYear());
        row.createCell(col++).setCellValue(str(p.getJobTitle()));
        row.createCell(col++).setCellValue(str(p.getCompany()));
        row.createCell(col++).setCellValue(str(p.getLocation()));
        row.createCell(col++).setCellValue(p.getSkills() != null ? String.join(", ", p.getSkills()) : "");
        row.createCell(col++).setCellValue(str(p.getResumeUrl()));
        row.createCell(col++).setCellValue(str(p.getPhotoUrl()));
        row.createCell(col++).setCellValue(str(p.getBloodGroup()));
        var s = p.getSocials();
        row.createCell(col++).setCellValue(s != null ? str(s.getLinkedinUrl()) : "");
        row.createCell(col++).setCellValue(s != null ? str(s.getGithubUrl()) : "");
        row.createCell(col++).setCellValue(s != null ? str(s.getInstagramUrl()) : "");
        row.createCell(col).setCellValue(p.isApproved() ? "Yes" : "No");
    }

    private CellStyle buildHeaderStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short) 11);
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setBorderBottom(BorderStyle.THIN);
        return s;
    }

    private static String str(String v) { return v == null ? "" : v; }
}