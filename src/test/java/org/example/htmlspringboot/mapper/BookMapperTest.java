package org.example.htmlspringboot.mapper;

import org.example.htmlspringboot.domain.Book;
import org.example.htmlspringboot.dto.BookDTO;
import org.example.htmlspringboot.dto.CreateBookDTO;
import org.example.htmlspringboot.dto.UpdateBookDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BookMapperTest {

    private final BookMapper mapper = new BookMapper();

    @Test
    @DisplayName("toEntity(CreateBookDTO) ska mappa alla fält korrekt")
    void toEntity_mapsAllFields() {
        CreateBookDTO dto = new CreateBookDTO();
        dto.setTitle("My Title");
        dto.setDescription("Desc");
        dto.setPublishedDate(LocalDate.of(2024, 1, 2));
        dto.setAuthor("Author");
        dto.setIsbn("1234567890");

        Book entity = mapper.toEntity(dto);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getTitle()).isEqualTo("My Title");
        assertThat(entity.getDescription()).isEqualTo("Desc");
        assertThat(entity.getPublishedDate()).isEqualTo(LocalDate.of(2024, 1, 2));
        assertThat(entity.getAuthor()).isEqualTo("Author");
        assertThat(entity.getIsbn()).isEqualTo("1234567890");
    }

    @Test
    @DisplayName("updateEntity(UpdateBookDTO, Book) ska uppdatera mål-entiteten")
    void updateEntity_updatesTarget() {
        Book target = new Book("Old", "Old d", LocalDate.of(2020, 1, 1), "A", "1111111111");

        UpdateBookDTO dto = new UpdateBookDTO();
        dto.setTitle("New");
        dto.setDescription("New d");
        dto.setPublishedDate(LocalDate.of(2022, 2, 2));
        dto.setAuthor("B");
        dto.setIsbn("2222222222");

        mapper.updateEntity(dto, target);

        assertThat(target.getTitle()).isEqualTo("New");
        assertThat(target.getDescription()).isEqualTo("New d");
        assertThat(target.getPublishedDate()).isEqualTo(LocalDate.of(2022, 2, 2));
        assertThat(target.getAuthor()).isEqualTo("B");
        assertThat(target.getIsbn()).isEqualTo("2222222222");
    }

    @Test
    @DisplayName("toDTO(Book) ska mappa alla fält korrekt")
    void toDTO_mapsAllFields() {
        Book entity = new Book("T", "D", LocalDate.of(2021, 3, 3), "Auth", "3333333333");
        entity.setId(10L);

        BookDTO dto = mapper.toDTO(entity);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getTitle()).isEqualTo("T");
        assertThat(dto.getDescription()).isEqualTo("D");
        assertThat(dto.getPublishedDate()).isEqualTo(LocalDate.of(2021, 3, 3));
        assertThat(dto.getAuthor()).isEqualTo("Auth");
        assertThat(dto.getIsbn()).isEqualTo("3333333333");
    }
}
