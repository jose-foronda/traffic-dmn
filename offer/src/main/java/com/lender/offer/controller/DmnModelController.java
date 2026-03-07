package com.lender.offer.controller;

import com.lender.offer.model.DmnModel;
import com.lender.offer.service.DmnEvaluationService;
import com.lender.offer.service.DmnModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dmn-models")
@RequiredArgsConstructor
public class DmnModelController {
    private final DmnModelService service;
    private final DmnEvaluationService evaluationService;

    @PostMapping("/upload")
    public ResponseEntity<DmnModel> uploadDmnFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("version") String version,
            @RequestParam(value = "createdBy", required = false) String createdBy) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            
            DmnModel model = new DmnModel();
            model.setName(name);
            model.setVersion(version);
            model.setContent(content);
            model.setCreatedBy(createdBy);
            
            return ResponseEntity.ok(service.save(model));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{dmnModelId}/evaluate")
    public ResponseEntity<Map<String, Object>> evaluate(
            @PathVariable Long dmnModelId,
            @RequestBody Map<String, Object> context) {
        try {
            Map<String, Object> result = evaluationService.evaluate(dmnModelId, context);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    public ResponseEntity<DmnModel> create(@RequestBody DmnModel model) {
        return ResponseEntity.ok(service.save(model));
    }

    @GetMapping
    public ResponseEntity<List<DmnModel>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DmnModel> getById(@PathVariable Long id) {
        DmnModel model = service.findById(id);
        return model != null ? ResponseEntity.ok(model) : ResponseEntity.notFound().build();
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<DmnModel>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(service.findByStatus(status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
