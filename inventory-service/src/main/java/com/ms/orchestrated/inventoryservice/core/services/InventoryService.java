package com.ms.orchestrated.inventoryservice.core.services;

import com.ms.orchestrated.inventoryservice.config.exceptions.ValidationException;
import com.ms.orchestrated.inventoryservice.core.dtos.Event;
import com.ms.orchestrated.inventoryservice.core.dtos.History;
import com.ms.orchestrated.inventoryservice.core.dtos.Order;
import com.ms.orchestrated.inventoryservice.core.dtos.OrderProducts;
import com.ms.orchestrated.inventoryservice.core.enums.ESagaStatus;
import com.ms.orchestrated.inventoryservice.core.model.Inventory;
import com.ms.orchestrated.inventoryservice.core.model.OrderInventory;
import com.ms.orchestrated.inventoryservice.core.producer.KafkaProducer;
import com.ms.orchestrated.inventoryservice.core.repository.InventoryRepository;
import com.ms.orchestrated.inventoryservice.core.repository.OrderInventoryRepository;
import com.ms.orchestrated.inventoryservice.core.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final String CURRENT_SOURCE = "INVENTORY_SERVICE";

    private final JsonUtil jsonUtil;
    private final KafkaProducer kafkaProducer;
    private final InventoryRepository inventoryRepository;
    private final OrderInventoryRepository orderInventoryRepository;

    public void updateInventory(Event event){
        try {
            checkCurrentValidation(event);
            createOrderInventory(event);
            updateInventory(event.getPayload());
            handleSuccess(event);
        } catch (Exception e){
            log.error("Error trying to update inventory: ", e);
            handleFailCurrentNotExecuted(event, e.getMessage());
        }
        kafkaProducer.senEvent(jsonUtil.toJson(event));
    }

    private void checkCurrentValidation(Event event){
        if (orderInventoryRepository.existsByOrderIdAndTransactionId(
                event.getPayload().getId(), event.getTransactionId())){
            throw new ValidationException("There's another transactionId for this validation.");
        }
    }

    private void createOrderInventory(Event event){
        event.getPayload()
                .getProducts()
                .forEach( product -> {
                    var inventory = findInventoryByProductCode(product.getProduct().getCode());
                    var orderInventory = createOrderInventory(event, product, inventory);
                    orderInventoryRepository.save(orderInventory);
                });
    }

    private OrderInventory createOrderInventory(Event event, OrderProducts product, Inventory inventory){
        return OrderInventory
                .builder()
                .inventory(inventory)
                .oldQuantity(inventory.getAvailable())
                .orderQuantity(product.getQuantity())
                .newQuantity(inventory.getAvailable() - product.getQuantity())
                .orderId(event.getPayload().getId())
                .transactionId(event.getTransactionId())
                .build();
    }

    private void updateInventory(Order order){
        order.getProducts().forEach(product -> {
            var inventory = findInventoryByProductCode(product.getProduct().getCode());
            checkInventory(inventory.getAvailable(), product.getQuantity());
            inventory.setAvailable(inventory.getAvailable() - product.getQuantity());
            inventoryRepository.save(inventory);
        });
    }

    private void checkInventory(int available, int orderQuantity){
        if (orderQuantity > available){
            throw new ValidationException("Product is out of stock!");
        }
    }

    private void handleSuccess(Event event){
        event.setStatus(ESagaStatus.SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Inventory updated successfully");
    }

    private void addHistory(Event event, String message){
        var history = History
                .builder()
                .source(event.getSource())
                .status(event.getStatus())
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
        event.addToHistory(history);
    }

    private void handleFailCurrentNotExecuted(Event event, String message){
        event.setStatus(ESagaStatus.ROLLBACK_PENDING);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Fail to update inventory".concat(message));
    }

    public void rollbackInventory(Event event){
        event.setStatus(ESagaStatus.FAIL);
        event.setSource(CURRENT_SOURCE);
        try {
            returnInventoryToPreviousValues(event);
            addHistory(event, "Rollback executed for inventory!");
        } catch (Exception e){
            addHistory(event, "Rollback not executed for inventory: ".concat(e.getMessage()));
        }
        kafkaProducer.senEvent(jsonUtil.toJson(event));
    }

    private void returnInventoryToPreviousValues(Event event){
        orderInventoryRepository.findByOrderIdAndTransactionId(event.getPayload().getId(), event.getTransactionId())
                .forEach(orderInventory -> {
                    var inventory = orderInventory.getInventory();
                    inventory.setAvailable(orderInventory.getOldQuantity());
                    inventoryRepository.save(inventory);
                    log.info("Restore inventory for order {}: from {} to {}",
                            event.getPayload().getId(), orderInventory.getNewQuantity(), inventory.getAvailable());
                });
    }

    private Inventory findInventoryByProductCode(String productCode){
        return inventoryRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ValidationException("Inventory not found by informed product."));
    }
}