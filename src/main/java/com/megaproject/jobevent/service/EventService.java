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

    /** Register the current user for an event (RSVP) */
    public EventResponse rsvp(String eventId, String userId) {
        Event event = findById(eventId);
        if (event.getRegisteredUserIds() == null) {
            event.setRegisteredUserIds(new java.util.ArrayList<>());
        }
        if (event.getRegisteredUserIds().contains(userId)) {
            return mapper.toEventResponse(event); // already registered
        }
        if (event.getMaxParticipants() != null && event.getRegisteredUserIds().size() >= event.getMaxParticipants()) {
            throw new IllegalStateException("Event is full — maximum " + event.getMaxParticipants() + " participants reached");
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
