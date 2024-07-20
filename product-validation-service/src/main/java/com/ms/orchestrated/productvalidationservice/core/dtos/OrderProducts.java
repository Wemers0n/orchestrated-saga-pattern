package com.ms.orchestrated.productvalidationservice.core.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderProducts {

    private Product product;
    private Integer quantity;
}
