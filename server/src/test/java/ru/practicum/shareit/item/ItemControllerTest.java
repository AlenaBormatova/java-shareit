package ru.practicum.shareit.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ItemControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;
    @Autowired
    ItemRepository itemRepository;
    @Autowired
    BookingRepository bookingRepository;

    private User owner;
    private User booker;
    private Item item;

    @BeforeEach
    void setup() {
        // Создание тестовых данных перед каждым тестом
        owner = userRepository.save(new User(null, "owner", "owner@example.com"));
        booker = userRepository.save(new User(null, "booker", "booker@example.com"));
        item = itemRepository.save(new Item(null, "Дрель", "удобная", true, owner, null));
    }

    // Тестирование успешного создания предмета с валидными данными
    @Test
    void createItemTest() throws Exception {
        ItemDto dto = new ItemDto(null, "Отвёртка", "крестовая", true, null);

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", owner.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Отвёртка"));
    }

    // Параметризованный тест, проверка валидации полей предмета
    @ParameterizedTest
    @MethodSource("invalidItemDtos")
    void createItemWithInvalidDataTest(ItemDto dto) throws Exception {
        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", owner.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    static Stream<Arguments> invalidItemDtos() {
        return Stream.of(
                Arguments.of(new ItemDto(null, null, "d", true, null)),
                Arguments.of(new ItemDto(null, "   ", "d", true, null)),
                Arguments.of(new ItemDto(null, "n", null, true, null)),
                Arguments.of(new ItemDto(null, "n", "   ", true, null)),
                Arguments.of(new ItemDto(null, "n", "d", null, null))
        );
    }

    // Тестирование создания предмета с несуществующим ID запроса
    @Test
    void createItemWithNonExistentRequestTest() throws Exception {
        ItemDto dto = new ItemDto(null, "Sth", "desc", true, 777L);

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", owner.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    // Тестирование попытки создания предмета с дублирующимся именем
    @Test
    void createItemWithDuplicateNameTest() throws Exception {
        ItemDto dto1 = new ItemDto(null, "dup", "d1", true, null);
        ItemDto dto2 = new ItemDto(null, "dup", "d2", true, null);

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", owner.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", owner.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    // Тестирование успешного частичного обновления предмета
    @Test
    void updateItemTest() throws Exception {
        ItemDto patch = new ItemDto(null, "Дрель-2", null, null, null);

        mockMvc.perform(patch("/items/{id}", item.getId())
                        .header("X-Sharer-User-Id", owner.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patch)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Дрель-2"));
    }

    // Тестирование получения предмета по ID
    @Test
    void getItemByIdTest() throws Exception {
        mockMvc.perform(get("/items/{id}", item.getId())
                        .header("X-Sharer-User-Id", owner.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(item.getId().intValue()))
                .andExpect(jsonPath("$.name").value("Дрель"))
                .andExpect(jsonPath("$.comments").isArray());
    }

    // Тестирование получения всех предметов владельца
    @Test
    void getAllItemsByOwnerTest() throws Exception {
        itemRepository.save(new Item(null, "Молоток", "тяжёлый", true, owner, null));

        mockMvc.perform(get("/items")
                        .header("X-Sharer-User-Id", owner.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // Тестирование получения предмета владельцем с отображением бронирований и комментариев
    @Test
    void getItemByIdWithBookingsAndCommentsAsOwnerTest() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Booking past = bookingRepository.save(new Booking(null, now.minusHours(2), now.minusHours(1), item, booker, BookingStatus.APPROVED));
        Booking future = bookingRepository.save(new Booking(null, now.plusHours(1), now.plusHours(2), item, booker, BookingStatus.APPROVED));

        CommentDto comment = new CommentDto(null, "норм", null, null);
        mockMvc.perform(post("/items/{id}/comment", item.getId())
                        .header("X-Sharer-User-Id", booker.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comment)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/items/{id}", item.getId())
                        .header("X-Sharer-User-Id", owner.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(item.getId().intValue()))
                .andExpect(jsonPath("$.lastBooking").exists())
                .andExpect(jsonPath("$.lastBooking.id").value(past.getId().intValue()))
                .andExpect(jsonPath("$.lastBooking.bookerId").value(booker.getId().intValue()))
                .andExpect(jsonPath("$.nextBooking").exists())
                .andExpect(jsonPath("$.nextBooking.id").value(future.getId().intValue()))
                .andExpect(jsonPath("$.nextBooking.bookerId").value(booker.getId().intValue()))
                .andExpect(jsonPath("$.comments", hasSize(1)))
                .andExpect(jsonPath("$.comments[0].text").value("норм"))
                .andExpect(jsonPath("$.comments[0].authorName").value("booker"));
    }

    // Тестирование получения предмета НЕ владельцем
    @Test
    void getItemByIdAsNonOwnerTest() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        bookingRepository.save(new Booking(null, now.minusHours(2), now.minusHours(1), item, booker, BookingStatus.APPROVED));
        bookingRepository.save(new Booking(null, now.plusHours(1), now.plusHours(2), item, booker, BookingStatus.APPROVED));

        CommentDto comment = new CommentDto(null, "коммент", null, null);
        mockMvc.perform(post("/items/{id}/comment", item.getId())
                        .header("X-Sharer-User-Id", booker.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comment)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/items/{id}", item.getId())
                        .header("X-Sharer-User-Id", booker.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(item.getId().intValue()))
                .andExpect(jsonPath("$.lastBooking").doesNotExist())
                .andExpect(jsonPath("$.nextBooking").doesNotExist())
                .andExpect(jsonPath("$.comments", hasSize(1)));
    }

    // Тестирование получения списка предметов владельца с полной информацией о бронированиях и комментариях
    @Test
    void getAllItemsWithBookingsAndCommentsTest() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        bookingRepository.save(new Booking(null, now.minusHours(3), now.minusHours(2), item, booker, BookingStatus.APPROVED));
        bookingRepository.save(new Booking(null, now.plusHours(2), now.plusHours(3), item, booker, BookingStatus.APPROVED));

        CommentDto comment = new CommentDto(null, "ok", null, null);
        mockMvc.perform(post("/items/{id}/comment", item.getId())
                        .header("X-Sharer-User-Id", booker.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comment)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/items")
                        .header("X-Sharer-User-Id", owner.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(item.getId().intValue()))
                .andExpect(jsonPath("$[0].lastBooking").exists())
                .andExpect(jsonPath("$[0].nextBooking").exists())
                .andExpect(jsonPath("$[0].comments", hasSize(1)));
    }

    // Тестирование получения списка предметов, когда нет бронирований и комментариев
    @Test
    void getAllItemsWithoutBookingsTest() throws Exception {
        mockMvc.perform(get("/items")
                        .header("X-Sharer-User-Id", owner.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(item.getId().intValue()))
                .andExpect(jsonPath("$[0].comments", hasSize(0)));
    }

    // Тестирование успешного удаления предмета владельцем
    @Test
    void deleteItemTest() throws Exception {
        Item newItem = itemRepository.save(new Item(null, "Ключ", "набор", true, owner, null));

        mockMvc.perform(delete("/items/{id}", newItem.getId())
                        .header("X-Sharer-User-Id", owner.getId()))
                .andDo(print())
                .andExpect(status().isOk());

        mockMvc.perform(get("/items/{id}", newItem.getId())
                        .header("X-Sharer-User-Id", owner.getId()))
                .andExpect(status().isNotFound());
    }

    // Тестирование попытки удаления предмета НЕ владельцем
    @Test
    void deleteItemAsNonOwnerTest() throws Exception {
        Item someoneItem = itemRepository.save(new Item(null, "Набор бит", "PZ", true, owner, null));

        mockMvc.perform(delete("/items/{id}", someoneItem.getId())
                        .header("X-Sharer-User-Id", booker.getId()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    // Тестирование функциональности поиска предметов по тексту
    @Test
    void searchItemsTest() throws Exception {
        mockMvc.perform(get("/items/search")
                        .param("text", "дрель"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));
    }

    // Тестирование успешного добавления комментария к предмету
    @Test
    void addCommentToItemTest() throws Exception {
        Booking b = new Booking(null, LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1), item, booker, BookingStatus.APPROVED);
        bookingRepository.save(b);

        CommentDto dto = new CommentDto(null, "хорошая вещь", null, null);

        mockMvc.perform(post("/items/{itemId}/comment", item.getId())
                        .header("X-Sharer-User-Id", booker.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.text").value("хорошая вещь"))
                .andExpect(jsonPath("$.authorName").value("booker"));
    }

    // Тестирование создания предмета без обязательного заголовка X-Sharer-User-Id
    @Test
    void createItemWithoutUserHeaderTest() throws Exception {
        ItemDto dto = new ItemDto(null, "Пила", "ручная", true, null);

        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
