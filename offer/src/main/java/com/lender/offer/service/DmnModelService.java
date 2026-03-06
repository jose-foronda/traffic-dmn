package com.lender.offer.service;

import com.lender.offer.model.DmnModel;
import com.lender.offer.repository.DmnModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DmnModelService {
    private final DmnModelRepository repository;

    @Transactional
    public DmnModel save(DmnModel model) {
        return repository.save(model);
    }

    public List<DmnModel> findAll() {
        return repository.findAll();
    }

    public DmnModel findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public List<DmnModel> findByStatus(String status) {
        return repository.findByStatus(status);
    }

    public DmnModel findByNameAndVersion(String name, String version) {
        return repository.findByNameAndVersion(name, version).orElse(null);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
