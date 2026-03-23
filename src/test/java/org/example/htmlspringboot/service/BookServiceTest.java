package org.example.htmlspringboot.service;

import org.example.htmlspringboot.domain.Book;
import org.example.htmlspringboot.dto.BookDTO;
import org.example.htmlspringboot.dto.CreateBookDTO;
import org.example.htmlspringboot.dto.UpdateBookDTO;
import org.example.htmlspringboot.exception.ResourceNotFoundException;
import org.example.htmlspringboot.mapper.BookMapper;
import org.example.htmlspringboot.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class BookServiceTest {

    private BookRepository repository;
    private BookMapper mapper;
    private BookService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(BookRepository.class);
        mapper = new BookMapper();
        service = new BookService(repository, mapper);
    }

    @Test
    @DisplayName("listAll ska returnera mappade DTOs")
    void listAll_returnsMappedDTOs() {
        Book b = new Book("T", "D", LocalDate.of(2020,1,1), "A", "1111111111");
        b.setId(1L);
        when(repository.findAll()).thenReturn(List.of(b));

        List<BookDTO> result = service.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getTitle()).isEqualTo("T");
    }

    @Test
    @DisplayName("getById ska returnera DTO när den finns")
    void getById_returnsDTO() {
        Book b = new Book("T", "D", null, "A", "1111111111");
        b.setId(5L);
        when(repository.findById(5L)).thenReturn(Optional.of(b));

        BookDTO dto = service.getById(5L);

        assertThat(dto.getId()).isEqualTo(5L);
        assertThat(dto.getIsbn()).isEqualTo("1111111111");
    }

    @Test
    @DisplayName("getById ska kasta ResourceNotFoundException när den saknas")
    void getById_throwsWhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("create ska neka dublett-ISBN och spara annars")
    void create_enforcesUniqueIsbn() {
        CreateBookDTO input = new CreateBookDTO();
        input.setTitle("T");
        input.setAuthor("A");
        input.setIsbn("DUP");

        when(repository.existsByIsbn("DUP")).thenReturn(true);
        assertThatThrownBy(() -> service.create(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISBN");

        // Unik isbn -> spara
        reset(repository);
        when(repository.existsByIsbn("DUP")).thenReturn(false);
        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        Book saved = new Book("T", null, null, "A", "DUP");
        saved.setId(10L);
        when(repository.save(any(Book.class))).thenReturn(saved);

        BookDTO dto = service.create(input);

        verify(repository).save(bookCaptor.capture());
        assertThat(bookCaptor.getValue().getTitle()).isEqualTo("T");
        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getIsbn()).isEqualTo("DUP");
    }

    @Test
    @DisplayName("update ska kasta 404 om id saknas")
    void update_throwsWhenMissing() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        UpdateBookDTO input = new UpdateBookDTO();
        input.setIsbn("X");
        assertThatThrownBy(() -> service.update(1L, input))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("update ska neka när ISBN ändras till redan existerande")
    void update_duplicateIsbnOnChange_isRejected() {
        Book existing = new Book("T", null, null, "A", "111");
        existing.setId(2L);
        when(repository.findById(2L)).thenReturn(Optional.of(existing));

        UpdateBookDTO input = new UpdateBookDTO();
        input.setTitle("T2");
        input.setAuthor("A2");
        input.setIsbn("EXISTS");

        when(repository.existsByIsbn("EXISTS")).thenReturn(true);

        assertThatThrownBy(() -> service.update(2L, input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISBN");
    }

    @Test
    @DisplayName("update ska tillåta oförändrat ISBN och spara")
    void update_allowsUnchangedIsbn() {
        Book existing = new Book("T", null, null, "A", "SAME");
        existing.setId(3L);
        when(repository.findById(3L)).thenReturn(Optional.of(existing));

        UpdateBookDTO input = new UpdateBookDTO();
        input.setTitle("NewT");
        input.setAuthor("NewA");
        input.setIsbn("SAME");

        Book after = new Book("NewT", null, null, "NewA", "SAME");
        after.setId(3L);
        when(repository.save(existing)).thenReturn(after);

        BookDTO result = service.update(3L, input);

        assertThat(result.getTitle()).isEqualTo("NewT");
        assertThat(result.getAuthor()).isEqualTo("NewA");
        verify(repository, never()).existsByIsbn(anyString());
    }

    @Test
    @DisplayName("delete ska kasta 404 om id saknas och annars ta bort")
    void delete_behaviour() {
        when(repository.existsById(42L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(42L))
                .isInstanceOf(ResourceNotFoundException.class);

        reset(repository);
        when(repository.existsById(42L)).thenReturn(true);
        service.delete(42L);
        verify(repository).deleteById(42L);
    }
}
