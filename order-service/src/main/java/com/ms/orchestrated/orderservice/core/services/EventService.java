package com.ms.orchestrated.orderservice.core.services;

import com.ms.orchestrated.orderservice.config.exception.ValidationException;
import com.ms.orchestrated.orderservice.core.document.Event;
import com.ms.orchestrated.orderservice.core.dtos.EventFilters;
import com.ms.orchestrated.orderservice.core.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.util.ObjectUtils.isEmpty;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository repository;

    public void notifyEnding(Event event){
        event.setOrderId(event.getOrderId());
        event.setCreatedAt(LocalDateTime.now());
        save(event);
        log.info("Order {} with saga notified TransactionId: {}", event.getOrderId(), event.getTransactionId());
    }

    public List<Event> findAll(){
        return this.repository.findAllByOrderByCreatedAtDesc();
    }

    public Event findByFilters(EventFilters filters){
        validateEmptyFilters(filters);
        if (!isEmpty(filters.getOrderId())){
            return findByOrderId(filters.getOrderId());
        } else {
            return findByTransactionId(filters.getTransactionId());
        }
    }

    private Event findByOrderId(String orderId){
        return this.repository.findTop1ByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ValidationException("Event not found by orderId."));
    }

    private Event findByTransactionId(String transactionId){
        return this.repository.findTop1ByTransactionIdOrderByCreatedAtDesc(transactionId)
                .orElseThrow(() -> new ValidationException("Event not found by transactionId"));
    }

    private void validateEmptyFilters(EventFilters filters){
        if (isEmpty(filters.getOrderId()) && isEmpty(filters.getTransactionId())){
            throw new ValidationException("OrderId or TransactionId must be informed.");
        }
    }

    public Event save(Event event){
        return repository.save(event);
    }
}
