package com.lender.offer.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Service
public class ScesimTestService {

    public Map<String, Object> runScesimTest(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".scesim")) {
            throw new IllegalArgumentException("Invalid file type: must be .scesim");
        }
        
        Map<String, Object> result = new HashMap<>();
        
//        try {
//            String content = new String(file.getBytes());
//            ScenarioSimulationModel model = ScenarioSimulationXMLPersistence.getInstance().unmarshal(content);
//
//            AbstractRunnerHelper runnerHelper = new RuleScenarioRunnerHelper();
//            runnerHelper.run(model, getClass().getClassLoader());
//
//            result.put("testPassed", true);
//            result.put("totalScenarios", model.getSimulation().getScenarioWithIndex().size());
//            result.put("failedScenarios", 0);
//            result.put("fileName", file.getOriginalFilename());
//            result.put("message", "SCESIM executed successfully");
//
//        } catch (Exception e) {
//            result.put("testPassed", false);
//            result.put("error", e.getMessage());
//        }
        
        return result;
    }
}