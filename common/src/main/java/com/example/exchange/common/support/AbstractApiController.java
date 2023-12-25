package com.example.exchange.common.support;


import com.example.exchange.common.ApiError;
import com.example.exchange.common.ApiErrorResponse;
import com.example.exchange.common.ApiException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

public abstract class AbstractApiController extends LoggerSupport {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ApiException.class)
    @ResponseBody
    public ApiErrorResponse handleException(HttpServletResponse response, Exception ex) {
        response.setContentType("application/json;charset=utf-8");
        ApiException apiEx = null;
        if (ex instanceof ApiException) {
            apiEx = (ApiException) ex;
        } else {
            apiEx = new ApiException(ApiError.INTERNAL_SERVER_ERROR, null, ex.getMessage());
        }
        return apiEx.error;
    }
}
