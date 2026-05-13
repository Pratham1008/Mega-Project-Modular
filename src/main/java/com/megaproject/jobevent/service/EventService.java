package com.megaproject.jobevent.service;

import com.megaproject.jobevent.dto.request.EventRequest;
import com.megaproject.jobevent.dto.response.EventResponse;
import com.megaproject.jobevent.exception.ResourceNotFoundException;
import com.megaproject.jobevent.mapper.JobEventMapper;
import com.megaproject.jobevent.model.Event;
import com.megaproject.jobevent.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final JobEventMapper mapper;

    public EventResponse create(EventRequest req, String createdByUserId) {
        Event event = mapper.toEvent(req);
        event.setActive(true);
        event.setCreatedByUserId(createdByUserId);
        // Default targetAudience to ALL if not specified
        if (event.getTargetAudience() == null || event.getTargetAudience().isBlank()) {
            event.setTargetAudience("ALL");
        }
        return mapper.toEventResponse(eventRepository.save(event));
    }

    public EventResponse getById(String id) {
        return mapper.toEventResponse(findById(id));
    }

    public List<EventResponse> getAllActive() {
        return mapper.toEventResponseList(eventRepository.findByActiveTrue());
    }

    /** Paginated version for frontend infinite scroll */
    public Page<EventResponse> getAllActivePaged(Pageable pageable) {
        return eventRepository.findByActiveTrue(pageable).map(mapper::toEventResponse);
    }

    public List<EventResponse> getByCreator(String userId) {
        return mapper.toEventResponseList(
                eventRepository.findByCreatedByUserIdAndActiveTrue(userId));
    }

    public EventResponse update(String id, EventRequest req, String requesterId, String role) {
        Event event = findById(id);
        if (!event.getCreatedByUserId().equals(requesterId) && !"ADMIN".equals(role)) {
            throw new AccessDeniedException("You can only edit your own events");
        }
        mapper.updateEvent(req, event);
        return mapper.toEventResponse(eventRepository.save(event));
    }

    /** Register the current user for an event (RSVP) with audience enforcement */
    public EventResponse rsvp(String eventId, String userId, String userRole) {
        Event event = findById(eventId);

        // Enforce target audience restrictions
        String audience = event.getTargetAudience() != null ? event.getTargetAudience() : "ALL";
        if ("STUDENTS_ONLY".equals(audience) && !"STUDENT".equals(userRole)) {
            throw new AccessDeniedException("This event is for students only.");
        }
        if ("ALUMNI_ONLY".equals(audience) && !"ALUMNI".equals(userRole)) {
            throw new AccessDeniedException("This event is for alumni only.");
        }

        if (event.getRegisteredUserIds() == null) {
            event.setRegisteredUserIds(new java.util.ArrayList<>());
        }
        
        // Prevent duplicate registrations
        if (event.getRegisteredUserIds().contains(userId)) {
            return mapper.toEventResponse(event);
        }
        
        // Check capacity
        if (event.getMaxParticipants() != null && event.getRegisteredUserIds().size() >= event.getMaxParticipants()) {
            throw new IllegalStateException("Event is full — maximum " + event.getMaxParticipants() + " participants reached.");
        }
        
        event.getRegisteredUserIds().add(userId);
        return mapper.toEventResponse(eventRepository.save(event));
    }

    /** Unregister the current user from an event */
    public EventResponse unrsvp(String eventId, String userId) {
        Event event = findById(eventId);
        if (event.getRegisteredUserIds() != null) {
            event.getRegisteredUserIds().remove(userId);
            eventRepository.save(event);
        }
        return mapper.toEventResponse(event);
    }

    public void softDelete(String id) {
        Event event = findById(id);
        event.setActive(false);
        eventRepository.save(event);
    }

    private Event findById(String id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
    }
}

