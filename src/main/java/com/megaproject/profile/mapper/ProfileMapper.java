package com.megaproject.profile.mapper;

import com.megaproject.profile.dto.request.*;
import com.megaproject.profile.dto.response.*;
import com.megaproject.profile.model.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProfileMapper {

    // ---- EducationalProfileRequest → ProfileDocument ----
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "profileType", ignore = true)   // determined by service
    @Mapping(target = "approved", constant = "false")
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "designation", ignore = true)
    @Mapping(target = "officeLocation", ignore = true)
    @Mapping(target = "researchInterests", ignore = true)
    ProfileDocument toDocument(EducationalProfileRequest req);

    // ---- FacultyProfileRequest → ProfileDocument ----
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "profileType", constant = "FACULTY")
    @Mapping(target = "approved", constant = "false")
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "registrationNumber", ignore = true)
    @Mapping(target = "admissionYear", ignore = true)
    @Mapping(target = "passingYear", ignore = true)
    @Mapping(target = "currentSemester", ignore = true)
    @Mapping(target = "resumeUrl", ignore = true)
    @Mapping(target = "skills", ignore = true)
    @Mapping(target = "jobTitle", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "location", ignore = true)
    ProfileDocument toDocument(FacultyProfileRequest req);

    // ---- ProfileDocument → EducationalProfileResponse ----
    EducationalProfileResponse toEducationalResponse(ProfileDocument doc);

    // ---- ProfileDocument → FacultyProfileResponse ----
    FacultyProfileResponse toFacultyResponse(ProfileDocument doc);

    // ---- ProfileDocument → ProfileSummaryResponse ----
    ProfileSummaryResponse toSummary(ProfileDocument doc);

    // ---- ProfileDocument → AlumniSearchResponse (direct mapping, no ES layer) ----
    AlumniSearchResponse toAlumniSearchResponse(ProfileDocument doc);

    // ---- Address mappings ----
    Address toAddress(AddressRequest req);
    AddressResponse toAddressResponse(Address address);

    // ---- Socials mappings ----
    Socials toSocials(SocialsRequest req);
    SocialsResponse toSocialsResponse(Socials socials);

    // ---- Update existing document from EducationalProfileRequest (ignore nulls) ----
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "profileType", ignore = true)
    @Mapping(target = "approved", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "designation", ignore = true)
    @Mapping(target = "officeLocation", ignore = true)
    @Mapping(target = "researchInterests", ignore = true)
    void updateDocumentFromRequest(EducationalProfileRequest req, @MappingTarget ProfileDocument doc);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "profileType", ignore = true)
    @Mapping(target = "approved", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "registrationNumber", ignore = true)
    @Mapping(target = "admissionYear", ignore = true)
    @Mapping(target = "passingYear", ignore = true)
    @Mapping(target = "currentSemester", ignore = true)
    @Mapping(target = "resumeUrl", ignore = true)
    @Mapping(target = "skills", ignore = true)
    @Mapping(target = "jobTitle", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "location", ignore = true)
    void updateDocumentFromRequest(FacultyProfileRequest req, @MappingTarget ProfileDocument doc);
}
