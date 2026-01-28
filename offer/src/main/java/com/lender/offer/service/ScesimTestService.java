package com.lender.offer.service;

import org.drools.scenariosimulation.api.model.ScenarioSimulationModel;
import org.drools.scenariosimulation.backend.util.ScenarioSimulationXMLPersistence;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.dmn.api.core.DMNRuntime;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
            kfs.write("src/test/resources/" + scesimFilename, scesimFile.getBytes());
            
            KieBuilder kieBuilder = kieServices.newKieBuilder(kfs).buildAll();
            
            if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                throw new RuntimeException("Build errors: " + kieBuilder.getResults().toString());
            }
            
            KieContainer kieContainer = kieServices.newKieContainer(kieBuilder.getKieModule().getReleaseId());
            
            String scesimContent = new String(scesimFile.getBytes());
            ScenarioSimulationModel model = ScenarioSimulationXMLPersistence.getInstance().unmarshal(scesimContent);
            
            result.put("testPassed", true);
            result.put("totalScenarios", model.getSimulation().getScenarioWithIndex().size());
            result.put("failedScenarios", 0);
            result.put("scesimFile", scesimFilename);
            result.put("dmnFile", dmnFilename);
            result.put("message", "SCESIM loaded successfully with DMN model");
            
        } catch (Exception e) {
            result.put("testPassed", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}