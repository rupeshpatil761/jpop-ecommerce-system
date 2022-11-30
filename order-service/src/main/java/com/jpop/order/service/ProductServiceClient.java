package com.jpop.order.service;

import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

//@FeignClient(name = "product-service", fallback = FeignFallback.class)
@FeignClient(name = "product-service", configuration = MyFeignConfiguration.class)
//@FeignClient(name = "product-service")
public interface ProductServiceClient {
    @GetMapping("/product")
    ResponseEntity<Object> getAllProducts();
}
