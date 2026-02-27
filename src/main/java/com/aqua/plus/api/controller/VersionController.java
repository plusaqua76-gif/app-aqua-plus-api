package com.aqua.plus.api.controller;

import com.aqua.plus.commons.dtos.ResponseDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
@Tag(name = "VersionController - Controller", description = "Controller encargado de probar version")
@CrossOrigin(origins = "*", methods = { RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST,
        RequestMethod.PUT })
@RequiredArgsConstructor
public class VersionController {

    @Value("${spring.application.version}")
    private String version;


    @GetMapping
    public ResponseEntity<String> getVersion() {
        return new ResponseEntity<>(version, HttpStatus.OK);
    }
}
