package ru.practicum.shareit.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.shareit.booking.model.BookingStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDto {
    @NotNull(message = "ID бронирования не может быть null")
    private Long id;

    @NotNull(message = "Дата начала не может быть null")
    private LocalDateTime start;

    @NotNull(message = "Дата окончания не может быть null")
    private LocalDateTime end;

    @NotNull(message = "Статус не может быть null")
    private BookingStatus status;

    @Valid
    @NotNull(message = "Предмет не может быть null")
    private ItemDto item;

    @Valid
    @NotNull(message = "Бронирующий не может быть null")
    private BookerDto booker;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDto {
        @NotNull(message = "ID предмета не может быть null")
        private Long id;

        @NotNull(message = "Название предмета не может быть null")
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookerDto {
        @NotNull(message = "ID бронирующего не может быть null")
        private Long id;

        @NotNull(message = "Имя бронирующего не может быть null")
        private String name;
    }
}