package com.example.exchange.ui.web;

import com.example.exchange.UserContext;
import com.example.exchange.bean.AuthToken;
import com.example.exchange.support.AbstractFilter;
import com.example.exchange.user.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class UIFilterRegistrationBean extends FilterRegistrationBean<Filter> {
    @Autowired
    UserService userService;

    @Autowired
    CookieService cookieService;

    @PostConstruct
    public void init() {
        UIFilter filter = new UIFilter();
        setFilter(filter);
        addUrlPatterns("/*");
        setName(filter.getClass().getSimpleName());
        setOrder(100);
    }

    class UIFilter extends AbstractFilter {

        @Override
        public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) resp;
            String path = request.getRequestURI();
            if (logger.isDebugEnabled()) {
                logger.debug("process {} {}...", request.getMethod(), path);
            }
            // set default encoding:
            request.setCharacterEncoding("UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.setContentType("text/html;charset=UTF-8");
            // try parse user from cookie
            AuthToken auth = cookieService.findSessionCookie(request);
            if (auth != null && auth.isAboutToExpire()) {
                logger.info("refresh session cookie...");
                cookieService.setSessionCookie(request, response, auth.refresh());
            }
            Long userId = auth == null ? null : auth.userId();
            if (logger.isDebugEnabled()) {
                logger.debug("parsed user {} from session cookie.", userId);
            }
            try (UserContext ctx = new UserContext(userId)) {
                chain.doFilter(request, response);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
