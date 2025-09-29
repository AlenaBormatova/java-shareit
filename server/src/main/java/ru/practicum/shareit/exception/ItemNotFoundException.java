package ru.practicum.shareit.exception;

public class ItemNotFoundException extends NotFoundException {
    public ItemNotFoundException(String message) {
        super(message);
    }
}