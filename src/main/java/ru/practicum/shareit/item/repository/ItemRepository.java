package ru.practicum.shareit.item.repository;

import ru.practicum.shareit.item.model.Item;

import java.util.List;

public interface ItemRepository {
    Item save(Item item);

    Item update(Item item);

    Item findById(Long id);

    List<Item> findAll();

    List<Item> findByOwnerId(Long ownerId);

    List<Item> searchAvailableByNameOrDescription(String text);

    void deleteById(Long id);

    boolean existsByNameAndOwnerId(String name, Long ownerId);
}