package com.bank.transactionservice.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "account-service")
public interface AccountClient {

    @PostMapping("/api/v1/accounts/{id}/deposit")
    void deposit(@PathVariable("id") Long id,
                 @RequestParam BigDecimal amount);

    @PostMapping("/api/v1/accounts/{id}/withdraw")
    void withdraw(@PathVariable("id") Long id,
                  @RequestParam BigDecimal amount);
}