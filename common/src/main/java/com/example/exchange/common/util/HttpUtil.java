package com.example.exchange.common.util;

import jakarta.servlet.http.HttpServletRequest;

public class HttpUtil {
    public static boolean isSecure(HttpServletRequest request) {
        String forwarded = request.getHeader("x-forwarded-proto");
        if (forwarded != null) {
            return "https".equals(forwarded);
        }
        return "https".equals(request.getScheme());
    }
}
