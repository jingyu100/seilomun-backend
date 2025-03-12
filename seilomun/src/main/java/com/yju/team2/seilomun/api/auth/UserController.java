package com.yju.team2.seilomun.api.auth;

import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.service.CustomerService;
import com.yju.team2.seilomun.dto.ApiResponseJson;
import com.yju.team2.seilomun.dto.CustomerRegisterDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

@Slf4j
@ResponseStatus
@RequiredArgsConstructor
public class UserController {

    private final CustomerService customerService;

    @PostMapping("/api/account/register")
    public ApiResponseJson registerNewAccount(@Valid @RequestBody CustomerRegisterDto customerRegisterDto,
                                              BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }

        Customer customer = customerService.registerCustomer(customerRegisterDto);
        log.info("Account successfully registered with details: {}.", customer);

        return new ApiResponseJson(HttpStatus.OK, Map.of(
                "email", customer.getEmail(),
                "username", customer.getName()
        ));
    }
}
