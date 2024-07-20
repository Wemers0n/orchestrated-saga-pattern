package com.ms.orchestrated.productvalidationservice.core.services;

import com.ms.orchestrated.productvalidationservice.config.exceptions.ValidationException;
import com.ms.orchestrated.productvalidationservice.core.dtos.Event;
import com.ms.orchestrated.productvalidationservice.core.dtos.History;
import com.ms.orchestrated.productvalidationservice.core.dtos.OrderProducts;
import com.ms.orchestrated.productvalidationservice.core.enums.ESagaStatus;
import com.ms.orchestrated.productvalidationservice.core.model.Validation;
import com.ms.orchestrated.productvalidationservice.core.producer.KafkaProducer;
import com.ms.orchestrated.productvalidationservice.core.repository.ProductRepository;
import com.ms.orchestrated.productvalidationservice.core.repository.ValidationRepository;
import com.ms.orchestrated.productvalidationservice.core.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static org.springframework.util.ObjectUtils.isEmpty;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductValidationService {

    private static final String CURRENT_SOURCE = "PRODUCT_VALIDATION_SERVICE";

    private final JsonUtil jsonUtil;
    private final KafkaProducer kafkaProducer;
    private final ProductRepository productRepository;
    private final ValidationRepository validationRepository;

    public void validateExistingProducts(Event event){
        try {
            checkCurrentValidation(event);
            createValidation(event, true);
            handleSuccess(event);
        } catch (Exception e){
            log.error("Error trying validate product: ", e);
            handleFailCurrentNotExecuted(event, e.getMessage());
        }
        kafkaProducer.sendEvent(jsonUtil.toJson(event));
    }

    private void validateProductInformed(Event event){
        if (isEmpty(event.getPayload()) || isEmpty(event.getPayload().getProducts())){
            throw new ValidationException("Product list is empty!");
        }
        if (isEmpty(event.getPayload().getId()) || isEmpty(event.getPayload().getTransactionId())){
            throw new ValidationException("OrderId and TransactionId must be informed!");
        }
    }

    private void checkCurrentValidation(Event event){
        validateProductInformed(event);
        if (validationRepository.existsByOrderIdAndTransactionId(
                event.getOrderId(), event.getTransactionId())){
            throw new ValidationException("There's another transactionId for this validation.");
        }
        event.getPayload().getProducts().forEach(product -> {
            validateProductInformed(product);
            validateExistingProduct(product.getProduct().getCode());
        });
    }

    private void validateProductInformed(OrderProducts products){
        if(isEmpty(products.getProduct()) || isEmpty(products.getProduct().getCode())){
            throw new ValidationException("Product must be informed.");
        }
    }

    private void validateExistingProduct(String code){
        if (!productRepository.existsByCode(code)){
            throw new ValidationException("Product does not exists in database!");
        }
    }

    private void createValidation(Event event, boolean success){
        var validation = Validation
                .builder()
                .orderId(event.getPayload().getId())
                .transactionId(event.getTransactionId())
                .success(success)
                .build();
        validationRepository.save(validation);
    }

    private void handleSuccess(Event event){
        event.setStatus(ESagaStatus.SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Products are validation successfully!");
    }

    private void addHistory(Event event, String message){
        var history = History
                .builder()
                .source(event.getSource())
                .status(event.getStatus())
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
        event.addHistory(history);
    }

    private void handleFailCurrentNotExecuted(Event event, String message){
        event.setStatus(ESagaStatus.ROLLBACK_PENDING);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Fail to validate products: ".concat(message));
    }

    public void rollbackEvent(Event event){
        changeValidationToFail(event);
        event.setStatus(ESagaStatus.FAIL);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Rollback executed on product validation!");
        kafkaProducer.sendEvent(jsonUtil.toJson(event));
    }

    private void changeValidationToFail(Event event){
        validationRepository.findByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId())
                .ifPresentOrElse(validation -> {
                    validation.setSuccess(false);
                    validationRepository.save(validation);
                }, () -> createValidation(event, false));
    }

}
