package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.*;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemDtoWithBookings;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final UserService userService;
    private final ItemRepository itemRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;
    private final ItemRequestRepository itemRequestRepository;

    @Override
    @Transactional
    public ItemDto createItem(Long ownerId, ItemDto itemDto) {
        validateItem(itemDto);

        if (!userService.userExists(ownerId)) {
            throw new UserNotFoundException("Пользователь с ID " + ownerId + " не найден");
        }

        checkForDuplicateItem(ownerId, itemDto.getName()); // Проверка на дубликат (вещь с таким же названием у того же владельца)

        User owner = getUser(ownerId); // Проверка существования пользователя

        ItemRequest request = null;
        if (itemDto.getRequestId() != null) {
            request = itemRequestRepository.findById(itemDto.getRequestId())
                    .orElseThrow(() -> new NotFoundException("Запрос не найден"));
        }

        Item item = ItemMapper.toItem(itemDto, owner, request);
        item = itemRepository.save(item);

        return ItemMapper.toItemDto(item);
    }

    @Override
    @Transactional
    public ItemDto updateItem(Long ownerId, Long itemId, ItemDto itemDto) {
        Item existingItem = getItemById(itemId);
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

        itemRepository.save(existingItem);
        return ItemMapper.toItemDto(existingItem);
    }

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long itemId, CommentDto commentDto) {
        User author = getUser(userId);
        Item item = getItemById(itemId);

        // Проверяем, что пользователь действительно брал вещь в аренду
        boolean hasBooked = bookingRepository.findCompletedBookingsByUser(itemId, userId, LocalDateTime.now()).size() > 0;

        if (!hasBooked) {
            throw new ValidationException("Пользователь не брал эту вещь в аренду");
        }

        if (commentDto.getText() == null || commentDto.getText().isBlank()) {
            throw new ValidationException("Текст комментария не может быть пустым");
        }

        Comment comment = new Comment();
        comment.setText(commentDto.getText());
        comment.setItem(item);
        comment.setAuthor(author);
        comment.setCreated(LocalDateTime.now());

        comment = commentRepository.save(comment);

        CommentDto result = new CommentDto();
        result.setId(comment.getId());
        result.setText(comment.getText());
        result.setAuthorName(comment.getAuthor().getName());
        result.setCreated(comment.getCreated());

        return result;
    }

    @Override
    public ItemDto getItem(Long itemId) {
        Item item = getItemById(itemId); // Проверка существования вещи
        return ItemMapper.toItemDto(item);
    }

    @Override
    public ItemDtoWithBookings getItemWithBookings(Long itemId, Long userId) {
        Item item = getItemById(itemId);
        ItemDtoWithBookings result = convertToItemDtoWithBookings(item);

        // Добавляем информацию о бронированиях только для владельца
        if (item.getOwner().getId().equals(userId)) {
            LocalDateTime now = LocalDateTime.now();

            // Последнее бронирование
            List<Booking> lastBookings = bookingRepository.findLastBookings(itemId, now);
            if (!lastBookings.isEmpty()) {
                Booking lastBooking = lastBookings.getFirst();
                result.setLastBooking(new ItemDtoWithBookings.BookingInfo(
                        lastBooking.getId(),
                        lastBooking.getBooker().getId(),
                        lastBooking.getStart(),
                        lastBooking.getEnd()
                ));
            }

            // Следующее бронирование
            List<Booking> nextBookings = bookingRepository.findNextBookings(itemId, now);
            if (!nextBookings.isEmpty()) {
                Booking nextBooking = nextBookings.getFirst();
                result.setNextBooking(new ItemDtoWithBookings.BookingInfo(
                        nextBooking.getId(),
                        nextBooking.getBooker().getId(),
                        nextBooking.getStart(),
                        nextBooking.getEnd()
                ));
            }
        }

        // Добавляем комментарии
        List<Comment> comments = commentRepository.findByItemId(itemId);
        result.setComments(comments.stream()
                .map(comment -> new CommentDto(
                        comment.getId(),
                        comment.getText(),
                        comment.getAuthor().getName(),
                        comment.getCreated()
                ))
                .collect(Collectors.toList()));

        return result;
    }

    @Override
    public List<ItemDto> getUserItems(Long ownerId) {
        getUser(ownerId);
        return itemRepository.findByOwnerIdOrderById(ownerId).stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemDtoWithBookings> getUserItemsWithBookings(Long ownerId) {
        getUser(ownerId);
        List<Item> items = itemRepository.findByOwnerIdOrderById(ownerId);
        LocalDateTime now = LocalDateTime.now();

        return items.stream()
                .map(item -> {
                    ItemDtoWithBookings dto = convertToItemDtoWithBookings(item);

                    // Последнее бронирование
                    List<Booking> lastBookings = bookingRepository.findLastBookings(item.getId(), now);
                    if (!lastBookings.isEmpty()) {
                        Booking lastBooking = lastBookings.getFirst();
                        dto.setLastBooking(new ItemDtoWithBookings.BookingInfo(
                                lastBooking.getId(),
                                lastBooking.getBooker().getId(),
                                lastBooking.getStart(),
                                lastBooking.getEnd()
                        ));
                    }

                    // Следующее бронирование
                    List<Booking> nextBookings = bookingRepository.findNextBookings(item.getId(), now);
                    if (!nextBookings.isEmpty()) {
                        Booking nextBooking = nextBookings.getFirst();
                        dto.setNextBooking(new ItemDtoWithBookings.BookingInfo(
                                nextBooking.getId(),
                                nextBooking.getBooker().getId(),
                                nextBooking.getStart(),
                                nextBooking.getEnd()
                        ));
                    }

                    // Комментарии
                    List<Comment> comments = commentRepository.findByItemId(item.getId());
                    dto.setComments(comments.stream()
                            .map(comment -> new CommentDto(
                                    comment.getId(),
                                    comment.getText(),
                                    comment.getAuthor().getName(),
                                    comment.getCreated()
                            ))
                            .collect(Collectors.toList()));

                    return dto;
                })
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
    @Transactional
    public void deleteItem(Long ownerId, Long itemId) {
        Item item = getItemById(itemId); // Проверка существования вещи
        getUser(ownerId); // Проверка существования пользователя
        checkOwnership(item, ownerId);
        itemRepository.deleteById(itemId);
    }

    private Item getItemById(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Вещь с ID " + itemId + " не найдена"));
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
        Optional<Item> existingItem = itemRepository.findByOwnerIdAndName(ownerId, itemName);
        if (existingItem.isPresent()) {
            throw new ConflictException("Вещь с названием '" + itemName + "' уже существует у пользователя с ID " + ownerId);
        }
    }

    private ItemDtoWithBookings convertToItemDtoWithBookings(Item item) {
        ItemDtoWithBookings dto = new ItemDtoWithBookings();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setAvailable(item.getAvailable());
        dto.setRequestId(item.getRequest() != null ? item.getRequest().getId() : null);
        return dto;
    }
}