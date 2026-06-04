package com.megaproject.event.mapper;

import com.megaproject.event.dto.request.EventRequest;
import com.megaproject.event.dto.response.EventResponse;
import com.megaproject.event.model.Event;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EventMapper {

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
    @Mapping(target = "createdByUserId", ignore = true)
    @Mapping(target = "registeredUserIds", ignore = true)
    @Mapping(target = "organizerName", ignore = true)
    @Mapping(target = "organizerContact", ignore = true)
    void updateEvent(EventRequest req, @MappingTarget Event event);
}
