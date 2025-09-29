package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingState;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookingControllerTest {

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

    // Тестирование для создания бронирования
    @Test
    void createBookingTest() throws Exception {
        BookingRequestDto dto = new BookingRequestDto(item.getId(), LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", booker.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.start").exists())
                .andExpect(jsonPath("$.end").exists())
                .andExpect(jsonPath("$.status").exists());
    }

    // Тестирование подтверждения бронирования
    @Test
    void approveBookingTest() throws Exception {
        BookingRequestDto dto = new BookingRequestDto(item.getId(), LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));
        String resp = mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", booker.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andReturn().getResponse().getContentAsString();
        long bookingId = objectMapper.readTree(resp).get("id").asLong();

        mockMvc.perform(patch("/bookings/{id}", bookingId)
                        .header("X-Sharer-User-Id", owner.getId())
                        .param("approved", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    // Тестирование получения бронирования по ID
    @Test
    void getBookingByIdTest() throws Exception {
        BookingRequestDto dto = new BookingRequestDto(item.getId(), LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));
        String resp = mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", booker.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andReturn().getResponse().getContentAsString();
        long bookingId = objectMapper.readTree(resp).get("id").asLong();

        mockMvc.perform(get("/bookings/{id}", bookingId)
                        .header("X-Sharer-User-Id", booker.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) bookingId));
    }

    // Тестирование получения списка бронирований пользователя
    @Test
    void getUserBookingsTest() throws Exception {
        BookingRequestDto dto = new BookingRequestDto(item.getId(), LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));
        mockMvc.perform(post("/bookings")
                .header("X-Sharer-User-Id", booker.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)));

        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", booker.getId())
                        .param("state", "ALL")
                        .param("from", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));
    }

    // Тестирование получения списка бронирований владельца
    @Test
    void getOwnerBookingsTest() throws Exception {
        BookingRequestDto dto = new BookingRequestDto(item.getId(), LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));
        mockMvc.perform(post("/bookings")
                .header("X-Sharer-User-Id", booker.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)));

        mockMvc.perform(get("/bookings/owner")
                        .header("X-Sharer-User-Id", owner.getId())
                        .param("state", "ALL")
                        .param("from", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));
    }

    // Параметризованные тесты для разных состояний
    @ParameterizedTest
    @EnumSource(value = BookingState.class, names = {"ALL", "CURRENT", "PAST", "FUTURE", "WAITING", "REJECTED"})
    void getUserBookingsByStateTest(BookingState state) throws Exception {
        seedDataForState(state);
        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", booker.getId())
                        .param("state", state.name())
                        .param("from", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));
    }

    @ParameterizedTest
    @EnumSource(value = BookingState.class, names = {"ALL", "CURRENT", "PAST", "FUTURE", "WAITING", "REJECTED"})
    void getOwnerBookingsByStateTest(BookingState state) throws Exception {
        seedDataForState(state);
        mockMvc.perform(get("/bookings/owner")
                        .header("X-Sharer-User-Id", owner.getId())
                        .param("state", state.name())
                        .param("from", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));
    }

    // Тест обработки ошибок
    @Test
    void missingHeaderBadRequestTest() throws Exception {
        mockMvc.perform(get("/bookings"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // Метод подготовки данных для состояний
    private void seedDataForState(BookingState state) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        switch (state) {
            case ALL -> {
                BookingRequestDto dto = new BookingRequestDto(item.getId(), now.plusHours(1), now.plusHours(2));
                mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", booker.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)));
            }
            case CURRENT -> {
                Booking b = new Booking(null, now.minusMinutes(10), now.plusMinutes(10), item, booker, BookingStatus.APPROVED);
                bookingRepository.save(b);
            }
            case PAST -> {
                Booking b = new Booking(null, now.minusHours(2), now.minusHours(1), item, booker, BookingStatus.APPROVED);
                bookingRepository.save(b);
            }
            case FUTURE -> {
                Booking b = new Booking(null, now.plusHours(2), now.plusHours(3), item, booker, BookingStatus.APPROVED);
                bookingRepository.save(b);
            }
            case WAITING -> {
                Booking b = new Booking(null, now.plusHours(1), now.plusHours(2), item, booker, BookingStatus.WAITING);
                bookingRepository.save(b);
            }
            case REJECTED -> {
                Booking b = new Booking(null, now.plusHours(1), now.plusHours(2), item, booker, BookingStatus.REJECTED);
                bookingRepository.save(b);
            }
        }
    }
}