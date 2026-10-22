package com.gamelog.service;

import com.gamelog.exception.ResourceNotFoundException;
import com.gamelog.models.Genre;
import com.gamelog.repository.GenreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GenreService {

    private final GenreRepository repository;

    public GenreService(GenreRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<Genre> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Genre findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Género no encontrado: " + id));
    }

    public Genre create(Genre data) {
        data.setId(null);
        return repository.saveAndFlush(data);
    }

    public Genre update(Long id, Genre data) {
        Genre existing = findById(id);
        existing.setName(data.getName());
        existing.setDescription(data.getDescription());
        return repository.saveAndFlush(existing);
    }

    public void delete(Long id) {
        repository.delete(findById(id));
        repository.flush(); // fuerza el error 409 si algún juego aún lo usa
    }
}
