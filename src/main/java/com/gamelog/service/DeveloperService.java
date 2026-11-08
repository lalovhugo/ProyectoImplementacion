package com.gamelog.service;

import com.gamelog.exception.ResourceNotFoundException;
import com.gamelog.models.Developer;
import com.gamelog.repository.DeveloperRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeveloperService {

    private final DeveloperRepository repository;

    public DeveloperService(DeveloperRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<Developer> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Developer findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Desarrollador no encontrado: " + id));
    }

    public Developer create(Developer data) {
        data.setId(null);
        return repository.saveAndFlush(data);
    }

    public Developer update(Long id, Developer data) {
        Developer existing = findById(id);
        existing.setName(data.getName());
        existing.setCountry(data.getCountry());
        return repository.saveAndFlush(existing);
    }

    public void delete(Long id) {
        repository.delete(findById(id));
        repository.flush();
    }
}
