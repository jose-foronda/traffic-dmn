package com.lender.offer.repository;

import com.lender.offer.model.DmnModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DmnModelRepository extends JpaRepository<DmnModel, Long> {
    List<DmnModel> findByStatus(String status);
    Optional<DmnModel> findByNameAndVersion(String name, String version);
    List<DmnModel> findByName(String name);
}
