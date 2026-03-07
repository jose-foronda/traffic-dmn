package com.lender.offer.service;

import com.lender.offer.model.DmnModel;
import lombok.RequiredArgsConstructor;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNResult;
import org.kie.dmn.api.core.DMNRuntime;
import org.kie.internal.io.ResourceFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class DmnEvaluationService {
    private final DmnModelService dmnModelService;

    public Map<String, Object> evaluate(Long dmnModelId, Map<String, Object> context) {
        DmnModel dmnModel = dmnModelService.findById(dmnModelId);
        if (dmnModel == null) {
            throw new IllegalArgumentException("DMN model not found: " + dmnModelId);
        }

        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kfs = kieServices.newKieFileSystem();
        
        Resource resource = ResourceFactory
            .newByteArrayResource(dmnModel.getContent().getBytes());
        resource.setSourcePath(dmnModel.getName() + ".dmn");
        kfs.write(resource);

        KieBuilder kieBuilder = kieServices.newKieBuilder(kfs).buildAll();
        KieContainer kieContainer = kieServices.newKieContainer(kieBuilder.getKieModule().getReleaseId());
        DMNRuntime runtime = kieContainer.newKieSession().getKieRuntime(DMNRuntime.class);

        DMNModel model = runtime.getModels().get(0);
        DMNContext dmnContext = runtime.newContext();
        context.forEach(dmnContext::set);

        DMNResult result = runtime.evaluateAll(model, dmnContext);
        
        return result.getContext().getAll();
    }
}
