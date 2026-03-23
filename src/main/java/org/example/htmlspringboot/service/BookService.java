package org.example.htmlspringboot.service;

import org.example.htmlspringboot.domain.Book;
import org.example.htmlspringboot.dto.BookDTO;
import org.example.htmlspringboot.dto.CreateBookDTO;
import org.example.htmlspringboot.dto.UpdateBookDTO;
import org.example.htmlspringboot.exception.ResourceNotFoundException;
import org.example.htmlspringboot.mapper.BookMapper;
import org.example.htmlspringboot.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BookService {

    private final BookRepository repository;
    private final BookMapper mapper;

    public BookService(BookRepository repository, BookMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<BookDTO> listAll() {
        return repository.findAll().stream()
                .map(mapper::toDTO)
                .toList();
    }

    public Page<BookDTO> listPaged(String q, Pageable pageable) {
        Pageable effective = pageable == null ? PageRequest.of(0, 10) : pageable;
        if (q != null && !q.isBlank()) {
            return repository
                    .findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(q, q, effective)
                    .map(mapper::toDTO);
        }
        var page = repository.findAll(effective).map(mapper::toDTO);
        return page;
    }

    public BookDTO getById(Long id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Book with id %d not found".formatted(id)));
    }

    public BookDTO create(CreateBookDTO input) {
        // Affärsregel: ISBN måste vara unikt
        if (repository.existsByIsbn(input.getIsbn())) {
            throw new IllegalArgumentException("A book with the same ISBN already exists");
        }
        Book entity = mapper.toEntity(input);
        Book saved = repository.save(entity);
        return mapper.toDTO(saved);
    }

    public BookDTO update(Long id, UpdateBookDTO input) {
        Book existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book with id %d not found".formatted(id)));

        // Om ISBN ändras, verifiera unikhet
        if (input.getIsbn() != null && !input.getIsbn().equals(existing.getIsbn())) {
            if (repository.existsByIsbn(input.getIsbn())) {
                throw new IllegalArgumentException("A book with the same ISBN already exists");
            }
        }

        mapper.updateEntity(input, existing);
        Book saved = repository.save(existing);
        return mapper.toDTO(saved);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Book with id %d not found".formatted(id));
        }
        repository.deleteById(id);
    }

    public Optional<BookDTO> findByIsbn(String isbn) {
        return repository.findByIsbn(isbn).map(mapper::toDTO);
    }
}
