package org.example.htmlspringboot.web;

import org.example.htmlspringboot.dto.BookDTO;
import org.example.htmlspringboot.service.BookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.example.htmlspringboot.exception.ResourceNotFoundException;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookControllerTest {

    private MockMvc buildMockMvc(BookService bookService) {
        // Ställ in en enkel vyresolver för Thymeleaf-namn -> JSP/HTML mock (vi pekar bara på templates-sökvägen)
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        BookController controller = new BookController(bookService);
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    @DisplayName("GET /books ska returnera vy 'books/list' och model-attribut 'books'")
    void listBooks_returnsViewAndModel() throws Exception {
        var dto = new BookDTO(1L, "Testbok", "Desc", LocalDate.of(2024,1,1), "Författare", "1234567890");
        BookService bookService = new FakeBookService(List.of(dto));
        MockMvc mockMvc = buildMockMvc(bookService);

        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/list"))
                .andExpect(model().attributeExists("books"))
                .andExpect(model().attribute("books", hasSize(1)))
                .andExpect(model().attribute("books", hasItem(
                        allOf(
                                hasProperty("id", is(1L)),
                                hasProperty("title", is("Testbok")),
                                hasProperty("author", is("Författare"))
                        )
                )));
    }

    @Test
    @DisplayName("GET /books/new ska visa formulär-vyn 'books/new'")
    void newBookForm_returnsView() throws Exception {
        BookService bookService = new FakeBookService(List.of());
        MockMvc mockMvc = buildMockMvc(bookService);
        mockMvc.perform(get("/books/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/new"))
                .andExpect(model().attributeExists("createBook"));
    }

    @Test
    @DisplayName("POST /books utan obligatoriska fält ska visa valideringsfel och stanna på 'books/new'")
    void createBook_validationErrors() throws Exception {
        BookService bookService = new FakeBookService(List.of());
        MockMvc mockMvc = buildMockMvc(bookService);
        mockMvc.perform(post("/books")
                        .param("title", "") // NotBlank
                        .param("author", "") // NotBlank
                        .param("isbn", "") // NotBlank
                )
                .andExpect(status().isOk())
                .andExpect(view().name("books/new"))
                .andExpect(model().attributeHasFieldErrors("createBook", "title", "author", "isbn"));
    }

    @Test
    @DisplayName("GET /books/{id}/edit ska fylla UpdateBookDTO och visa 'books/edit'")
    void editForm_populatesDto() throws Exception {
        var dto = new BookDTO(5L, "Titel", "Beskrivning", LocalDate.of(2023,5,5), "Anna", "1111111111");
        var svc = new ConfigurableFakeService().withGetById(dto);
        MockMvc mockMvc = buildMockMvc(svc);

        mockMvc.perform(get("/books/{id}/edit", 5L))
                .andExpect(status().isOk())
                .andExpect(view().name("books/edit"))
                .andExpect(model().attributeExists("updateBook"))
                .andExpect(model().attribute("updateBook", hasProperty("id", is(5L))))
                .andExpect(model().attribute("updateBook", hasProperty("title", is("Titel"))))
                .andExpect(model().attribute("updateBook", hasProperty("isbn", is("1111111111"))));
    }

    @Test
    @DisplayName("GET /books/{id}/edit för okänt id ska rendera 404-sida")
    void editForm_notFound_renders404() throws Exception {
        var svc = new ConfigurableFakeService().withNotFoundOnGet(99L);
        MockMvc mockMvc = buildMockMvc(svc);

        mockMvc.perform(get("/books/{id}/edit", 99L))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attributeExists("message"));
    }

    @Test
    @DisplayName("POST /books/{id}/edit med valideringsfel ska stanna på 'books/edit'")
    void update_validationErrors() throws Exception {
        var dto = new BookDTO(7L, "X", "", null, "Y", "2222222222");
        var svc = new ConfigurableFakeService().withGetById(dto);
        MockMvc mockMvc = buildMockMvc(svc);

        mockMvc.perform(post("/books/{id}/edit", 7L)
                        .param("id", "7")
                        .param("title", "") // NotBlank
                        .param("author", "") // NotBlank
                        .param("isbn", "") // NotBlank
                )
                .andExpect(status().isOk())
                .andExpect(view().name("books/edit"))
                .andExpect(model().attributeHasFieldErrors("updateBook", "title", "author", "isbn"));
    }

    @Test
    @DisplayName("POST /books/{id}/edit lyckas ska redirecta till /books")
    void update_success_redirects() throws Exception {
        var dto = new BookDTO(8L, "T", "", null, "A", "3333333333");
        var svc = new ConfigurableFakeService().withGetById(dto); // update gör inget fel
        MockMvc mockMvc = buildMockMvc(svc);

        mockMvc.perform(post("/books/{id}/edit", 8L)
                        .param("id", "8")
                        .param("title", "Ny titel")
                        .param("author", "Ny")
                        .param("isbn", "3333333333")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"));
    }

    @Test
    @DisplayName("POST /books/{id}/edit med dublett-ISBN ska visa fältfel på isbn")
    void update_duplicateIsbn_showsFieldError() throws Exception {
        var dto = new BookDTO(9L, "T", "", null, "A", "3333333333");
        var svc = new ConfigurableFakeService().withGetById(dto).withDuplicateOnUpdate(9L);
        MockMvc mockMvc = buildMockMvc(svc);

        mockMvc.perform(post("/books/{id}/edit", 9L)
                        .param("id", "9")
                        .param("title", "Ny")
                        .param("author", "A")
                        .param("isbn", "DUPLICATE")
                )
                .andExpect(status().isOk())
                .andExpect(view().name("books/edit"))
                .andExpect(model().attributeHasFieldErrors("updateBook", "isbn"));
    }

    @Test
    @DisplayName("POST /books/{id}/delete lyckas ska redirecta till /books")
    void delete_success_redirects() throws Exception {
        var svc = new ConfigurableFakeService();
        MockMvc mockMvc = buildMockMvc(svc);

        mockMvc.perform(post("/books/{id}/delete", 12L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"));
    }

    @Test
    @DisplayName("POST /books/{id}/delete för okänt id ska rendera 404-sida")
    void delete_notFound_renders404() throws Exception {
        var svc = new ConfigurableFakeService().withNotFoundOnDelete(77L);
        MockMvc mockMvc = buildMockMvc(svc);

        mockMvc.perform(post("/books/{id}/delete", 77L))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    // Minimal fake för att undvika Mockito/agent-problem på JDK 25
    private static class FakeBookService extends org.example.htmlspringboot.service.BookService {
        private final List<BookDTO> list;

        FakeBookService(List<BookDTO> list) {
            super(null, null);
            this.list = list;
        }

        @Override
        public List<BookDTO> listAll() {
            return list;
        }
    }

    private static class ConfigurableFakeService extends org.example.htmlspringboot.service.BookService {
        private BookDTO getByIdDto;
        private Long notFoundGetId;
        private Long duplicateOnUpdateId;
        private Long notFoundDeleteId;

        ConfigurableFakeService() { super(null, null); }

        ConfigurableFakeService withGetById(BookDTO dto) { this.getByIdDto = dto; return this; }
        ConfigurableFakeService withNotFoundOnGet(Long id) { this.notFoundGetId = id; return this; }
        ConfigurableFakeService withDuplicateOnUpdate(Long id) { this.duplicateOnUpdateId = id; return this; }
        ConfigurableFakeService withNotFoundOnDelete(Long id) { this.notFoundDeleteId = id; return this; }

        @Override
        public List<BookDTO> listAll() { return List.of(); }

        @Override
        public BookDTO getById(Long id) {
            if (notFoundGetId != null && notFoundGetId.equals(id)) {
                throw new ResourceNotFoundException("Book not found");
            }
            return getByIdDto;
        }

        @Override
        public BookDTO update(Long id, org.example.htmlspringboot.dto.UpdateBookDTO input) {
            if (duplicateOnUpdateId != null && duplicateOnUpdateId.equals(id)) {
                throw new IllegalArgumentException("A book with the same ISBN already exists");
            }
            return getByIdDto;
        }

        @Override
        public void delete(Long id) {
            if (notFoundDeleteId != null && notFoundDeleteId.equals(id)) {
                throw new ResourceNotFoundException("Book not found");
            }
        }
    }
}
