package com.example.exchange.common;

public record ApiErrorResponse(ApiError error, String data, String message) {
}
