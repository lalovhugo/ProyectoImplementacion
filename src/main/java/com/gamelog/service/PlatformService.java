package com.gamelog.service;

import com.gamelog.exception.ResourceNotFoundException;
import com.gamelog.models.Platform;
import com.gamelog.repository.PlatformRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlatformService {

    private final PlatformRepository repository;

    public PlatformService(PlatformRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<Platform> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Platform findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plataforma no encontrado: " + id));
    }

    public Platform create(Platform data) {
        data.setId(null);
        return repository.saveAndFlush(data);
    }

    public Platform update(Long id, Platform data) {
        Platform existing = findById(id);
        existing.setName(data.getName());
        existing.setManufacturer(data.getManufacturer());
        return repository.saveAndFlush(existing);
    }

    public void delete(Long id) {
        repository.delete(findById(id));
        repository.flush(); // fuerza el error 409 si algún juego aún lo usa
    }
}
