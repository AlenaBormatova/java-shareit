package ru.practicum.shareit.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.user.dto.UserDto;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    // Тестирование успешного создания пользователя
    @Test
    void createUserTest() throws Exception {
        UserDto dto = new UserDto(null, "u1", "u1@example.com");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("u1"))
                .andExpect(jsonPath("$.email").value("u1@example.com"));
    }

    // Тестирование обновления пользователя
    @Test
    void updateUserTest() throws Exception {
        UserDto dto = new UserDto(null, "u1", "u1@example.com");
        String resp = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(resp).get("id").asLong();

        UserDto patch = new UserDto(null, "u1-new", null);

        mockMvc.perform(patch("/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patch)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("u1-new"))
                .andExpect(jsonPath("$.email").value("u1@example.com"));
    }

    // Тестирование получения пользователя и списка
    @Test
    void getUserAndGetAllUsersTest() throws Exception {
        // Создание двух пользователей
        UserDto firstUser = new UserDto(null, "firstUser", "a@example.com");
        UserDto secondUser = new UserDto(null, "secondUser", "b@example.com");

        // Создание первого пользователя и получение его ID
        String r1 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstUser)))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(r1).get("id").asLong();

        // Создание второго пользователя
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondUser)));

        // Тестирование получения пользователя по ID
        mockMvc.perform(get("/users/{id}", id))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("firstUser"));

        // Тестирование получения списка всех пользователей
        mockMvc.perform(get("/users"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // Тестирование удаления пользователя
    @Test
    void deleteUserTest() throws Exception {
        UserDto dto = new UserDto(null, "u1", "u1@example.com");
        String resp = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(resp).get("id").asLong();

        mockMvc.perform(delete("/users/{id}", id))
                .andDo(print())
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/{id}", id))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}
