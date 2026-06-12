package com.aqua.plus.api.utils;

import org.springframework.stereotype.Component;

import com.aqua.plus.commons.dtos.SecureRequestDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecureRequestValidator {

    private final SecureRequestService secureRequestService;

    public String validate(SecureRequestDTO request) {
        return secureRequestService.validarPeticion(request);
    }
}
