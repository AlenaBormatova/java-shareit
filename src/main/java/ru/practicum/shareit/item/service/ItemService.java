package ru.practicum.shareit.item.service;

import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemDtoWithBookings;

import java.util.List;

public interface ItemService {
    ItemDto createItem(Long ownerId, ItemDto itemDto);

    ItemDto updateItem(Long ownerId, Long itemId, ItemDto itemDto);

    CommentDto addComment(Long userId, Long itemId, CommentDto commentDto);

    ItemDto getItem(Long itemId);

    ItemDtoWithBookings getItemWithBookings(Long itemId, Long userId);

    List<ItemDto> getUserItems(Long ownerId);

    List<ItemDtoWithBookings> getUserItemsWithBookings(Long ownerId);

    List<ItemDto> searchItems(String text);

    void deleteItem(Long ownerId, Long itemId);
}