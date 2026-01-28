package com.lender.offer.controller;

import com.lender.offer.service.DmnValidationService;
import com.lender.offer.service.ScesimTestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/dmn")
public class DmnController {

    private final DmnValidationService dmnValidationService;
    private final ScesimTestService scesimTestService;

    public DmnController(DmnValidationService dmnValidationService, ScesimTestService scesimTestService) {
        this.dmnValidationService = dmnValidationService;
        this.scesimTestService = scesimTestService;
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateDmnModel(@RequestParam("file") MultipartFile file) {
        try {
            var result = dmnValidationService.validateModel(file);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Validation failed");
        }
    }

    @PostMapping("/test-scesim")
    public ResponseEntity<?> testScesimFile(@RequestParam("file") MultipartFile file) {
        try {
            var result = scesimTestService.runScesimTest(file);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Test execution failed");
        }
    }
}