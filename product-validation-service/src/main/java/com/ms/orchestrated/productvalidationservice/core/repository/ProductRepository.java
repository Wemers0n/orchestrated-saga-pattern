package com.ms.orchestrated.productvalidationservice.core.repository;

import com.ms.orchestrated.productvalidationservice.core.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    Boolean existsByCode(String code);
}
