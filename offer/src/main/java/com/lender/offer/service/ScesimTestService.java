package com.lender.offer.service;

import org.drools.scenariosimulation.api.model.*;
import org.drools.scenariosimulation.backend.expression.ExpressionEvaluatorFactory;
import org.drools.scenariosimulation.backend.runner.DMNScenarioRunnerHelper;
import org.drools.scenariosimulation.backend.runner.model.ScenarioRunnerData;
import org.drools.scenariosimulation.backend.util.ScenarioSimulationXMLPersistence;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
public class ScesimTestService {

    private static class PublicDMNRunner extends DMNScenarioRunnerHelper {
        public void runScenario(KieContainer kieContainer, ScesimModelDescriptor descriptor, 
                                ScenarioWithIndex scenarioWithIndex, ExpressionEvaluatorFactory factory,
                                Settings settings, Background background, ScenarioRunnerData data) {
            super.run(kieContainer, descriptor, scenarioWithIndex, factory, 
                     getClass().getClassLoader(), data, settings, background);
        }
    }

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
            kfs.write("src/test/resources/" + scesimFilename, scesimFile.getBytes());
            
            KieBuilder kieBuilder = kieServices.newKieBuilder(kfs).buildAll();
            
            if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                throw new RuntimeException("Build errors: " + kieBuilder.getResults().toString());
            }
            
            KieContainer kieContainer = kieServices.newKieContainer(kieBuilder.getKieModule().getReleaseId());
            
            String scesimContent = new String(scesimFile.getBytes());
            ScenarioSimulationModel model = ScenarioSimulationXMLPersistence.getInstance().unmarshal(scesimContent);
            
            PublicDMNRunner runner = new PublicDMNRunner();
            ExpressionEvaluatorFactory factory = ExpressionEvaluatorFactory.create(
                getClass().getClassLoader(), ScenarioSimulationModel.Type.DMN);
            
            int totalScenarios = 0;
            int failedScenarios = 0;
            List<Map<String, Object>> scenarioResults = new ArrayList<>();
            
            Simulation simulation = model.getSimulation();
            Background background = model.getBackground();
            
            for (ScenarioWithIndex scenarioWithIndex : simulation.getScenarioWithIndex()) {
                totalScenarios++;
                Scenario scenario = scenarioWithIndex.getScesimData();
                
                try {
                    ScenarioRunnerData scenarioRunnerData = new ScenarioRunnerData();
                    runner.runScenario(kieContainer, simulation.getScesimModelDescriptor(),
                                     scenarioWithIndex, factory, model.getSettings(), 
                                     background, scenarioRunnerData);
                    
                    Map<String, Object> scenarioResult = new HashMap<>();
                    scenarioResult.put("index", scenarioWithIndex.getIndex());
                    scenarioResult.put("name", scenario.getDescription());
                    scenarioResult.put("passed", true);
                    scenarioResults.add(scenarioResult);
                    
                } catch (Exception e) {
                    failedScenarios++;
                    Map<String, Object> scenarioResult = new HashMap<>();
                    scenarioResult.put("index", scenarioWithIndex.getIndex());
                    scenarioResult.put("name", scenario.getDescription());
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
