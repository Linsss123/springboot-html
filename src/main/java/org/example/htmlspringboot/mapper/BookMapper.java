package org.example.htmlspringboot.mapper;

import org.example.htmlspringboot.domain.Book;
import org.example.htmlspringboot.dto.BookDTO;
import org.example.htmlspringboot.dto.CreateBookDTO;
import org.example.htmlspringboot.dto.UpdateBookDTO;
import org.springframework.stereotype.Component;

@Component
public class BookMapper {

    public Book toEntity(CreateBookDTO dto) {
        if (dto == null) return null;
        Book b = new Book();
        b.setTitle(dto.getTitle());
        b.setDescription(dto.getDescription());
        b.setPublishedDate(dto.getPublishedDate());
        b.setAuthor(dto.getAuthor());
        b.setIsbn(dto.getIsbn());
        return b;
    }

    public void updateEntity(UpdateBookDTO dto, Book target) {
        if (dto == null || target == null) return;
        target.setTitle(dto.getTitle());
        target.setDescription(dto.getDescription());
        target.setPublishedDate(dto.getPublishedDate());
        target.setAuthor(dto.getAuthor());
        target.setIsbn(dto.getIsbn());
    }

    public BookDTO toDTO(Book entity) {
        if (entity == null) return null;
        return new BookDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getPublishedDate(),
                entity.getAuthor(),
                entity.getIsbn()
        );
    }
}
