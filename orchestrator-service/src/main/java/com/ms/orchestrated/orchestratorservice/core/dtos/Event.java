package com.ms.orchestrated.orchestratorservice.core.dtos;

import com.ms.orchestrated.orchestratorservice.core.enums.EEventSource;
import com.ms.orchestrated.orchestratorservice.core.enums.ESagaStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static org.springframework.util.ObjectUtils.isEmpty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Event {

    private String id;
    private String transactionId;
    private String orderId;
    private Order payload;
    private EEventSource source;
    private ESagaStatus status;
    private List<History> eventHistory;
    private LocalDateTime createdAt;

    public void addToHistory(History history){
        if(isEmpty(eventHistory)){
            eventHistory = new ArrayList<>();
        }
        eventHistory.add(history);
    }
}
