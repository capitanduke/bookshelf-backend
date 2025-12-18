package com.readersnetwork.bookshelf.exception;

public class BookshelfNotFoundException extends RuntimeException {
    public BookshelfNotFoundException(String message) {
        super(message);
    }
}