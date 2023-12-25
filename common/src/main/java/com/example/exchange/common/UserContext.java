package com.example.exchange.common;

public class UserContext implements AutoCloseable {

    static final ThreadLocal<Long> THREAD_LOCAL_CTX = new ThreadLocal<>();


    public static Long getRequiredUserId() {
        Long userId = getUserId();
        if (userId == null) {
            throw new ApiException(ApiError.AUTH_SIGNIN_REQUIRED, null, "Need signin first.");
        }
        return userId;
    }

    public static Long getUserId() {
        return THREAD_LOCAL_CTX.get();
    }


    public UserContext(Long userId) {
        THREAD_LOCAL_CTX.set(userId);
    }

    @Override
    public void close() throws Exception {
        THREAD_LOCAL_CTX.remove();
    }
}
