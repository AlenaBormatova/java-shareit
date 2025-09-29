package ru.practicum.shareit.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ItemRequestControllerTest {

    private static final String BASE_URL = "/requests";
    private static final String USER_HEADER = "X-Sharer-User-Id";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;
    @Autowired
    ItemRequestRepository itemRequestRepository;
    @Autowired
    ItemRepository itemRepository;

    private User u1;
    private User u2;

    @BeforeEach
    void setUp() {
        // Создание двух тестовых пользователей перед каждым тестом
        u1 = userRepository.save(new User(null, "user1", "user1@example.com"));
        u2 = userRepository.save(new User(null, "user2", "user2@example.com"));
    }

    // Тестирование успешного создания запроса
    @Test
    void createItemRequestTest() throws Exception {
        ItemRequestDto body = new ItemRequestDto(null, "Нужен перфоратор", null, null);

        mockMvc.perform(post(BASE_URL)
                        .header(USER_HEADER, u1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.description").value("Нужен перфоратор"))
                .andExpect(jsonPath("$.created").exists())
                .andExpect(jsonPath("$.items").isArray());
    }

    // Тестирование создания запроса с несуществующим пользователем
    @Test
    void createItemRequestWithNonExistentUserTest() throws Exception {
        ItemRequestDto body = new ItemRequestDto(null, "Нужен шуруповёрт", null, null);

        mockMvc.perform(post(BASE_URL)
                        .header(USER_HEADER, 9999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    // Тестирование создания запроса без заголовка пользователя
    @Test
    void createItemRequestWithoutUserHeaderTest() throws Exception {
        ItemRequestDto body = new ItemRequestDto(null, "Нужен лобзик", null, null);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // Тестирование получения собственных запросов без предметов
    @Test
    void getOwnItemRequestsWithoutItemsTest() throws Exception {
        itemRequestRepository.save(new ItemRequest(null, "Нужна дрель", u1, LocalDateTime.now()));

        mockMvc.perform(get(BASE_URL)
                        .header(USER_HEADER, u1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].description").value("Нужна дрель"))
                .andExpect(jsonPath("$[0].items").isArray())
                .andExpect(jsonPath("$[0].items.length()").value(0));
    }

    // Тестирование получения собственных запросов с предметами
    @Test
    void getOwnItemRequestsWithItemsTest() throws Exception {
        ItemRequest r = itemRequestRepository.save(new ItemRequest(null, "Нужен перфоратор", u1, LocalDateTime.now().minusHours(1)));
        itemRepository.save(new Item(null, "Перфоратор", "Боевитый", true, u1, r));

        mockMvc.perform(get(BASE_URL)
                        .header(USER_HEADER, u1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Нужен перфоратор"))
                .andExpect(jsonPath("$[0].items").isArray())
                .andExpect(jsonPath("$[0].items.length()").value(1))
                .andExpect(jsonPath("$[0].items[0].name").value("Перфоратор"))
                .andExpect(jsonPath("$[0].items[0].ownerId").value(u1.getId().intValue()))
                .andExpect(jsonPath("$[0].items[0].requestId").value(r.getId().intValue()));
    }

    // Тестирование получения собственных запросов с несуществующим ID пользователя
    @Test
    void getOwnItemRequestsWithNonExistentUserTest() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .header(USER_HEADER, 123456L))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    // Тестирование получения собственных запросов без заголовка пользователя
    @Test
    void getOwnItemRequestsWithoutUserHeaderTest() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // Тестирование получения всех запросов с пагинацией
    @Test
    void getAllItemRequestsWithPaginationTest() throws Exception {
        itemRequestRepository.save(new ItemRequest(null, "Нужен молоток", u2, LocalDateTime.now().minusHours(2)));
        itemRequestRepository.save(new ItemRequest(null, "Нужна отвёртка", u2, LocalDateTime.now().minusHours(1)));
        itemRequestRepository.save(new ItemRequest(null, "u1 внутренний", u1, LocalDateTime.now().minusHours(3)));

        mockMvc.perform(get(BASE_URL + "/all")
                        .header(USER_HEADER, u1.getId())
                        .param("from", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].description").value("Нужна отвёртка"))
                .andExpect(jsonPath("$[1].description").value("Нужен молоток"));
    }

    // Тестирование получения всех запросов с несуществующим пользователем
    @Test
    void getAllItemRequestsWithNonExistentUserTest() throws Exception {
        mockMvc.perform(get(BASE_URL + "/all")
                        .header(USER_HEADER, 99999L)
                        .param("from", "0")
                        .param("size", "5"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    // Тестирование получения всех запросов без заголовка пользователя
    @Test
    void getAllItemRequestsWithoutUserHeaderTest() throws Exception {
        mockMvc.perform(get(BASE_URL + "/all")
                        .param("from", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // Тестирование получения конкретного запроса по ID
    @Test
    void getItemRequestByIdTest() throws Exception {
        ItemRequest r = itemRequestRepository.save(new ItemRequest(null, "Нужна стремянка", u2, LocalDateTime.now().minusMinutes(30)));
        itemRepository.save(new Item(null, "Стремянка", "2 м", true, u1, r));

        mockMvc.perform(get(BASE_URL + "/" + r.getId())
                        .header(USER_HEADER, u1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(r.getId().intValue()))
                .andExpect(jsonPath("$.description").value("Нужна стремянка"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Стремянка"));
    }

    // Тестирование получения несуществующего запроса по ID
    @Test
    void getNonExistentItemRequestByIdTest() throws Exception {
        mockMvc.perform(get(BASE_URL + "/424242")
                        .header(USER_HEADER, u1.getId()))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    // Тестирование получения запроса по ID с несуществующим пользователем
    @Test
    void getItemRequestByIdWithNonExistentUserTest() throws Exception {
        ItemRequest r = itemRequestRepository.save(new ItemRequest(null, "Нужен набор бит", u2, LocalDateTime.now()));

        mockMvc.perform(get(BASE_URL + "/" + r.getId())
                        .header(USER_HEADER, 9999L))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    // Тестирование получения запроса по ID без заголовка пользователя
    @Test
    void getItemRequestByIdWithoutUserHeaderTest() throws Exception {
        mockMvc.perform(get(BASE_URL + "/1"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
