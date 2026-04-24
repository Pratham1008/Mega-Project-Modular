package com.megaproject.jobevent.service;

import com.megaproject.jobevent.dto.request.EventRequest;
import com.megaproject.jobevent.dto.response.EventResponse;
import com.megaproject.jobevent.exception.ResourceNotFoundException;
import com.megaproject.jobevent.mapper.JobEventMapper;
import com.megaproject.jobevent.model.Event;
import com.megaproject.jobevent.repository.EventRepository;
import lombok.RequiredArgsConstructor;
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
