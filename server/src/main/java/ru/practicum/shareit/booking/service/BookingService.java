package ru.practicum.shareit.booking.service;

import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.booking.model.BookingState;

import java.util.List;

public interface BookingService {
    BookingResponseDto createBooking(Long bookerId, BookingRequestDto bookingRequestDto);

    BookingResponseDto updateBookingStatus(Long ownerId, Long bookingId, Boolean approved);

    BookingResponseDto getBookingById(Long userId, Long bookingId);

    List<BookingResponseDto> getUserBookings(Long bookerId, BookingState state, Integer from, Integer size);

    List<BookingResponseDto> getOwnerBookings(Long ownerId, BookingState state, Integer from, Integer size);
}