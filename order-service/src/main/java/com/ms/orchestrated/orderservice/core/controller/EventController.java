package com.ms.orchestrated.orderservice.core.controller;

import com.ms.orchestrated.orderservice.core.document.Event;
import com.ms.orchestrated.orderservice.core.dtos.EventFilters;
import com.ms.orchestrated.orderservice.core.services.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/event")
@RequiredArgsConstructor
public class EventController {

    private final EventService service;

    @GetMapping("/filter")
    public Event findByFilters(EventFilters filters){
        return this.service.findByFilters(filters);
    }

    @GetMapping("/all")
    public List<Event> findAll(){
        return this.service.findAll();
    }
}
