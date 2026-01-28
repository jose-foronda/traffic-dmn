package com.lender.offer.service;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.runtime.KieContainer;
import org.kie.dmn.api.core.DMNMessage;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNRuntime;
import org.kie.dmn.validation.DMNValidator;
import org.kie.dmn.validation.DMNValidatorFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DmnValidationService {

    public Map<String, Object> validateModel(MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize() > 10_000_000) {
            throw new IllegalArgumentException("Invalid file: empty or too large");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".dmn")) {
            throw new IllegalArgumentException("Invalid file type: must be .dmn");
        }
        
        byte[] fileBytes = file.getBytes();
        
        org.kie.api.io.Resource resource = org.kie.internal.io.ResourceFactory.newByteArrayResource(fileBytes);
        resource.setSourcePath(filename);
        
        DMNValidator validator = DMNValidatorFactory.newValidator();
        List<DMNMessage> messages = validator.validate(resource);
        
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kfs = kieServices.newKieFileSystem();
        kfs.write(resource);
        
        KieBuilder kieBuilder = kieServices.newKieBuilder(kfs).buildAll();
        KieContainer kieContainer = kieServices.newKieContainer(kieBuilder.getKieModule().getReleaseId());
        DMNRuntime runtime = kieContainer.newKieSession().getKieRuntime(DMNRuntime.class);
        
        List<DMNModel> models = runtime.getModels();
        DMNModel model = models.isEmpty() ? null : models.get(0);
        
        Map<String, Object> result = new HashMap<>();
        result.put("valid", messages.isEmpty() && model != null);
        result.put("modelName", model != null ? model.getName() : "Unknown");
        result.put("namespace", model != null ? model.getNamespace() : "Unknown");
        result.put("validationMessages", messages);
        
        return result;
    }
}