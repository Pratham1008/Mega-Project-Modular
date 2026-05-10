package com.megaproject.jobevent.mapper;

import com.megaproject.jobevent.dto.request.EventRequest;
import com.megaproject.jobevent.dto.request.JobRequest;
import com.megaproject.jobevent.dto.response.EventResponse;
import com.megaproject.jobevent.dto.response.JobResponse;
import com.megaproject.jobevent.model.Event;
import com.megaproject.jobevent.model.Job;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface JobEventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdByUserId", ignore = true)
    @Mapping(target = "registeredUserIds", ignore = true)
    @Mapping(target = "organizerName", ignore = true)
    @Mapping(target = "organizerContact", ignore = true)
    Event toEvent(EventRequest req);

    @Mapping(target = "registeredCount", expression = "java(event.getRegisteredUserIds() != null ? event.getRegisteredUserIds().size() : 0)")
    EventResponse toEventResponse(Event event);

    List<EventResponse> toEventResponseList(List<Event> events);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "postedByUserId", ignore = true)
    Job toJob(JobRequest req);

    JobResponse toJobResponse(Job job);

    List<JobResponse> toJobResponseList(List<Job> jobs);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdByUserId", ignore = true)
    @Mapping(target = "registeredUserIds", ignore = true)
    @Mapping(target = "organizerName", ignore = true)
    @Mapping(target = "organizerContact", ignore = true)
    void updateEvent(EventRequest req, @MappingTarget Event event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "postedByUserId", ignore = true)
    void updateJob(JobRequest req, @MappingTarget Job job);
}
