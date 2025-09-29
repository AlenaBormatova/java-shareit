package ru.practicum.shareit.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingState;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.*;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public BookingResponseDto createBooking(Long bookerId, BookingRequestDto bookingRequestDto) {
        User booker = userRepository.findById(bookerId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        Item item = itemRepository.findById(bookingRequestDto.getItemId())
                .orElseThrow(() -> new ItemNotFoundException("Вещь не найдена"));

        if (!item.getAvailable()) {
            throw new ValidationException("Вещь недоступна для бронирования");
        }

        if (item.getOwner().getId().equals(bookerId)) {
            throw new AccessDeniedException("Владелец не может бронировать свою вещь");
        }

        if (bookingRequestDto.getStart().isAfter(bookingRequestDto.getEnd()) ||
                bookingRequestDto.getStart().equals(bookingRequestDto.getEnd())) {
            throw new ValidationException("Некорректные даты бронирования");
        }

        Booking booking = new Booking();
        booking.setStart(bookingRequestDto.getStart());
        booking.setEnd(bookingRequestDto.getEnd());
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.WAITING);

        booking = bookingRepository.save(booking);
        return convertToResponseDto(booking);
    }

    @Override
    @Transactional
    public BookingResponseDto updateBookingStatus(Long ownerId, Long bookingId, Boolean approved) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование не найдено"));

        if (!booking.getItem().getOwner().getId().equals(ownerId)) {
            throw new AccessDeniedException("Только владелец может подтверждать бронирование");
        }

        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new ValidationException("Бронирование уже обработано");
        }

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        booking = bookingRepository.save(booking);

        return convertToResponseDto(booking);
    }

    @Override
    public BookingResponseDto getBookingById(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование не найдено"));

        // Проверка доступа
        if (!booking.getBooker().getId().equals(userId) &&
                !booking.getItem().getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Доступ запрещен");
        }

        return convertToResponseDto(booking);
    }

    @Override
    public List<BookingResponseDto> getUserBookings(Long bookerId, BookingState state, Integer from, Integer size) {
        if (!userRepository.existsById(bookerId)) {
            throw new UserNotFoundException("Пользователь не найден");
        }

        PageRequest pageRequest = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "start"));
        LocalDateTime now = LocalDateTime.now();

        switch (state) {
            case ALL:
                return bookingRepository.findByBookerId(bookerId, pageRequest)
                        .stream()
                        .map(this::convertToResponseDto)
                        .collect(Collectors.toList());
            case CURRENT:
                return bookingRepository.findByBookerIdAndStartBeforeAndEndAfter(
                                bookerId, now, now, pageRequest)
                        .stream()
                        .map(this::convertToResponseDto)
                        .collect(Collectors.toList());
            case PAST:
                return bookingRepository.findByBookerIdAndEndBefore(bookerId, now, pageRequest)
                        .stream()
                        .map(this::convertToResponseDto)
                        .collect(Collectors.toList());
            case FUTURE:
                return bookingRepository.findByBookerIdAndStartAfter(bookerId, now, pageRequest)
                        .stream()
                        .map(this::convertToResponseDto)
                        .collect(Collectors.toList());
            case WAITING:
                return bookingRepository.findByBookerIdAndStatus(bookerId, BookingStatus.WAITING, pageRequest)
                        .stream()
                        .map(this::convertToResponseDto)
                        .collect(Collectors.toList());
            case REJECTED:
                return bookingRepository.findByBookerIdAndStatus(bookerId, BookingStatus.REJECTED, pageRequest)
                        .stream()
                        .map(this::convertToResponseDto)
                        .collect(Collectors.toList());
            default:
                throw new ValidationException("Unknown state: " + state);
        }
    }

    @Override
    public List<BookingResponseDto> getOwnerBookings(Long ownerId, BookingState state, Integer from, Integer size) {
        if (!userRepository.existsById(ownerId)) {
            throw new UserNotFoundException("Пользователь не найден");
        }

        PageRequest pageRequest = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "start"));
        LocalDateTime now = LocalDateTime.now();

        switch (state) {
            case ALL:
                return bookingRepository.findByItemOwnerId(ownerId, pageRequest)
                        .stream()
                        .map(this::convertToResponseDto)
                        .collect(Collectors.toList());
            case CURRENT:
                return bookingRepository.findByItemOwnerIdAndStartBeforeAndEndAfter(
                                ownerId, now, now, pageRequest)
                        .stream()
                        .map(this::convertToResponseDto)
                        .collect(Collectors.toList());
            case PAST:
                return bookingRepository.findByItemOwnerIdAndEndBefore(ownerId, now, pageRequest)
                        .stream()
                        .map(this::convertToResponseDto)
                        .collect(Collectors.toList());
            case FUTURE:
                return bookingRepository.findByItemOwnerIdAndStartAfter(ownerId, now, pageRequest)
                        .stream()
                        .map(this::convertToResponseDto)
                        .collect(Collectors.toList());
            case WAITING:
                return bookingRepository.findByItemOwnerIdAndStatus(ownerId, BookingStatus.WAITING, pageRequest)
                        .stream()
                        .map(this::convertToResponseDto)
                        .collect(Collectors.toList());
            case REJECTED:
                return bookingRepository.findByItemOwnerIdAndStatus(ownerId, BookingStatus.REJECTED, pageRequest)
                        .stream()
                        .map(this::convertToResponseDto)
                        .collect(Collectors.toList());
            default:
                throw new ValidationException("Unknown state: " + state);
        }
    }

    private BookingResponseDto convertToResponseDto(Booking booking) {
        BookingResponseDto responseDto = new BookingResponseDto();
        responseDto.setId(booking.getId());
        responseDto.setStart(booking.getStart());
        responseDto.setEnd(booking.getEnd());
        responseDto.setStatus(booking.getStatus());

        // Полный объект item
        BookingResponseDto.ItemDto itemDto = new BookingResponseDto.ItemDto();
        itemDto.setId(booking.getItem().getId());
        itemDto.setName(booking.getItem().getName());
        responseDto.setItem(itemDto);

        // Полный объект booker
        BookingResponseDto.BookerDto bookerDto = new BookingResponseDto.BookerDto();
        bookerDto.setId(booking.getBooker().getId());
        bookerDto.setName(booking.getBooker().getName());
        responseDto.setBooker(bookerDto);

        return responseDto;
    }
}