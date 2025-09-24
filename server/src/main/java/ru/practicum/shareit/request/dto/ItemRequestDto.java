package ru.practicum.shareit.request.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemRequestDto {
    private Long id;
    private String description;
    private LocalDateTime created;
    private List<ItemResponseDto> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemResponseDto {
        private Long id;
        private String name;
        private Long ownerId;
        private String description;
        private Boolean available;
        private Long requestId;
    }
}