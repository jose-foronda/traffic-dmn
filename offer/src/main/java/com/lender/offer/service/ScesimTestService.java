package com.lender.offer.service;

import org.drools.scenariosimulation.api.model.*;
import org.drools.scenariosimulation.backend.util.ScenarioSimulationXMLPersistence;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.dmn.api.core.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
public class ScesimTestService {

    public Map<String, Object> runScesimTest(MultipartFile scesimFile, MultipartFile dmnFile) {
        if (scesimFile.isEmpty()) {
            throw new IllegalArgumentException("SCESIM file is empty");
        }
        if (dmnFile.isEmpty()) {
            throw new IllegalArgumentException("DMN file is empty");
        }
        
        String scesimFilename = scesimFile.getOriginalFilename();
        if (scesimFilename == null || !scesimFilename.toLowerCase(Locale.ROOT).endsWith(".scesim")) {
            throw new IllegalArgumentException("Invalid file type: must be .scesim");
        }
        
        String dmnFilename = dmnFile.getOriginalFilename();
        if (dmnFilename == null || !dmnFilename.toLowerCase(Locale.ROOT).endsWith(".dmn")) {
            throw new IllegalArgumentException("Invalid DMN file type: must be .dmn");
        }
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            KieServices kieServices = KieServices.Factory.get();
            KieFileSystem kfs = kieServices.newKieFileSystem();
            
            kfs.write("src/main/resources/" + dmnFilename, dmnFile.getBytes());
            
            KieBuilder kieBuilder = kieServices.newKieBuilder(kfs).buildAll();
            
            if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                throw new RuntimeException("Build errors: " + kieBuilder.getResults().toString());
            }
            
            KieContainer kieContainer = kieServices.newKieContainer(kieBuilder.getKieModule().getReleaseId());
            DMNRuntime dmnRuntime = kieContainer.newKieSession().getKieRuntime(DMNRuntime.class);
            
            String scesimContent = new String(scesimFile.getBytes());
            ScenarioSimulationModel model = ScenarioSimulationXMLPersistence.getInstance().unmarshal(scesimContent);
            
            List<DMNModel> dmnModels = dmnRuntime.getModels();
            if (dmnModels.isEmpty()) {
                throw new RuntimeException("No DMN model found");
            }
            DMNModel dmnModel = dmnModels.get(0);
            
            int totalScenarios = 0;
            int failedScenarios = 0;
            List<Map<String, Object>> scenarioResults = new ArrayList<>();
            
            Simulation simulation = model.getSimulation();
            for (ScenarioWithIndex scenarioWithIndex : simulation.getScenarioWithIndex()) {
                totalScenarios++;
                Scenario scenario = scenarioWithIndex.getScesimData();
                
                try {
                    DMNContext context = dmnRuntime.newContext();
                    Map<String, Object> inputsByName = new HashMap<>();
                    
                    for (FactMappingValue factMappingValue : scenario.getUnmodifiableFactMappingValues()) {
                        FactIdentifier factIdentifier = factMappingValue.getFactIdentifier();
                        ExpressionIdentifier expressionIdentifier = factMappingValue.getExpressionIdentifier();
                        
                        if (!factIdentifier.equals(FactIdentifier.EMPTY) && 
                            !factIdentifier.equals(FactIdentifier.INDEX) &&
                            !factIdentifier.equals(FactIdentifier.DESCRIPTION)) {
                            
                            String factName = factIdentifier.getName();
                            String propertyName = expressionIdentifier.getName();
                            Object value = factMappingValue.getRawValue();
                            
                            if (factName != null && !factName.isEmpty() && value != null) {
                                if (propertyName != null && !propertyName.isEmpty() && !propertyName.equals(factName)) {
                                    Map<String, Object> factObject = (Map<String, Object>) inputsByName.computeIfAbsent(factName, k -> new HashMap<>());
                                    factObject.put(propertyName, value);
                                } else {
                                    inputsByName.put(factName, value);
                                }
                            }
                        }
                    }
                    
                    for (Map.Entry<String, Object> entry : inputsByName.entrySet()) {
                        context.set(entry.getKey(), entry.getValue());
                    }
                    
                    DMNResult dmnResult = dmnRuntime.evaluateAll(dmnModel, context);
                    
                    boolean scenarioPassed = !dmnResult.hasErrors();
                    Map<String, Object> scenarioResult = new HashMap<>();
                    scenarioResult.put("index", scenarioWithIndex.getIndex());
                    scenarioResult.put("passed", scenarioPassed);
                    
                    if (!scenarioPassed) {
                        failedScenarios++;
                        scenarioResult.put("errors", dmnResult.getMessages().toString());
                    }
                    
                    scenarioResults.add(scenarioResult);
                    
                } catch (Exception e) {
                    failedScenarios++;
                    Map<String, Object> scenarioResult = new HashMap<>();
                    scenarioResult.put("index", scenarioWithIndex.getIndex());
                    scenarioResult.put("passed", false);
                    scenarioResult.put("error", e.getMessage());
                    scenarioResults.add(scenarioResult);
                }
            }
            
            result.put("testPassed", failedScenarios == 0);
            result.put("totalScenarios", totalScenarios);
            result.put("failedScenarios", failedScenarios);
            result.put("passedScenarios", totalScenarios - failedScenarios);
            result.put("scesimFile", scesimFilename);
            result.put("dmnFile", dmnFilename);
            result.put("scenarioResults", scenarioResults);
            result.put("message", "SCESIM executed: " + (totalScenarios - failedScenarios) + "/" + totalScenarios + " passed");
            
        } catch (Exception e) {
            result.put("testPassed", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}