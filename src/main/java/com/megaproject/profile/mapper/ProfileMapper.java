package com.megaproject.profile.mapper;

import com.megaproject.profile.dto.request.*;
import com.megaproject.profile.dto.response.*;
import com.megaproject.profile.model.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProfileMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "profileType", ignore = true)
    @Mapping(target = "approved", constant = "false")
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "designation", ignore = true)
    @Mapping(target = "officeLocation", ignore = true)
    @Mapping(target = "specialization", ignore = true)
    @Mapping(target = "degrees", ignore = true)
    @Mapping(target = "publications", ignore = true)
    ProfileDocument toDocument(EducationalProfileRequest req);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "profileType", constant = "FACULTY")
    @Mapping(target = "approved", constant = "false")
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "bloodGroup", ignore = true)
    @Mapping(target = "dateOfBirth", ignore = true)
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

    EducationalProfileResponse toEducationalResponse(ProfileDocument doc);

    FacultyProfileResponse toFacultyResponse(ProfileDocument doc);

    ProfileSummaryResponse toSummary(ProfileDocument doc);

    AlumniSearchResponse toAlumniSearchResponse(ProfileDocument doc);

    Address toAddress(AddressRequest req);
    AddressResponse toAddressResponse(Address address);

    Socials toSocials(SocialsRequest req);
    SocialsResponse toSocialsResponse(Socials socials);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "registrationNumber", ignore = true)
    @Mapping(target = "profileType", ignore = true)
    @Mapping(target = "approved", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "designation", ignore = true)
    @Mapping(target = "officeLocation", ignore = true)
    @Mapping(target = "specialization", ignore = true)
    @Mapping(target = "degrees", ignore = true)
    @Mapping(target = "publications", ignore = true)
    void updateDocumentFromRequest(EducationalProfileRequest req, @MappingTarget ProfileDocument doc);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "profileType", ignore = true)
    @Mapping(target = "approved", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "bloodGroup", ignore = true)
    @Mapping(target = "dateOfBirth", ignore = true)
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
