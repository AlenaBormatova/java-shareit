package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.*;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final UserService userService;
    private final ItemRepository itemRepository;

    @Override
    public ItemDto createItem(Long ownerId, ItemDto itemDto) {
        validateItem(itemDto);

        if (!userService.userExists(ownerId)) {
            throw new UserNotFoundException("Пользователь с ID " + ownerId + " не найден");
        }

        checkForDuplicateItem(ownerId, itemDto.getName()); // Проверка на дубликат (вещь с таким же названием у того же владельца)

        User owner = getUser(ownerId); // Проверка существования пользователя

        Item item = ItemMapper.toItem(itemDto, owner, null);
        item = itemRepository.save(item);

        return ItemMapper.toItemDto(item);
    }

    @Override
    public ItemDto updateItem(Long ownerId, Long itemId, ItemDto itemDto) {
        Item existingItem = itemRepository.findById(itemId); // Проверка существования вещи
        if (existingItem == null) {
            throw new NotFoundException("Item id = " + itemId + " не найден");
        }
        checkOwnership(existingItem, ownerId);

        if (itemDto.getName() != null) {
            existingItem.setName(itemDto.getName());
        }

        if (itemDto.getDescription() != null) {
            existingItem.setDescription(itemDto.getDescription());
        }

        if (itemDto.getAvailable() != null) {
            existingItem.setAvailable(itemDto.getAvailable());
        }

        itemRepository.update(existingItem);
        return ItemMapper.toItemDto(existingItem);
    }


    @Override
    public ItemDto getItem(Long itemId) {
        Item item = getItemById(itemId); // Проверка существования вещи
        return ItemMapper.toItemDto(item);
    }

    @Override
    public List<ItemDto> getUserItems(Long ownerId) {
        getUser(ownerId); // Проверка существования пользователя
        return itemRepository.findByOwnerId(ownerId).stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemDto> searchItems(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        return itemRepository.searchAvailableByNameOrDescription(text).stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteItem(Long ownerId, Long itemId) {
        Item item = getItemById(itemId); // Проверка существования вещи
        getUser(ownerId); // Проверка существования пользователя
        checkOwnership(item, ownerId);
        itemRepository.deleteById(itemId);
    }

    private Item getItemById(Long itemId) {
        Item item = itemRepository.findById(itemId);
        if (item == null) {
            throw new ItemNotFoundException("Вещь с ID " + itemId + " не найдена");
        }
        return item;
    }

    private User getUser(Long userId) {
        UserDto userDto = userService.getUser(userId); // Это выбросит UserNotFoundException если пользователь не найден
        return UserMapper.toUser(userDto);
    }

    private void validateItem(ItemDto itemDto) {
        if (itemDto.getName() == null || itemDto.getName().isBlank()) {
            throw new ValidationException("Название вещи не может быть пустым");
        }
        if (itemDto.getDescription() == null || itemDto.getDescription().isBlank()) {
            throw new ValidationException("Описание вещи не может быть пустым");
        }
        if (itemDto.getAvailable() == null) {
            throw new ValidationException("Статус доступности не может быть пустым");
        }
    }

    private void checkOwnership(Item item, Long ownerId) {
        if (!item.getOwner().getId().equals(ownerId)) {
            throw new AccessDeniedException("Только владелец может изменять вещь");
        }
    }

    private void checkForDuplicateItem(Long ownerId, String itemName) {
        if (itemRepository.existsByNameAndOwnerId(itemName, ownerId)) {
            throw new ConflictException("Вещь с названием '" + itemName + "' уже существует у пользователя с ID " + ownerId);
        }
    }
}