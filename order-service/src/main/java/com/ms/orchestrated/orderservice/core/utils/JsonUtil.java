package com.ms.orchestrated.orderservice.core.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.orchestrated.orderservice.core.document.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JsonUtil {

    private final ObjectMapper objectMapper;

    public String toJson(Object object){
        try{
            return objectMapper.writeValueAsString(object);
        } catch (Exception e){
            return "";
        }
    }

    public Event toEvent(String json){
        try{
            return objectMapper.readValue(json, Event.class);
        } catch (Exception e){
            return null;
        }
    }
}
