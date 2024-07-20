package com.ms.orchestrated.orderservice.core.repository;

import com.ms.orchestrated.orderservice.core.document.Order;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderRepository extends MongoRepository<Order, String> {
}
