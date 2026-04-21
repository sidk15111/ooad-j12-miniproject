package com.pes.smartqueue.config;

import com.pes.smartqueue.service.UserManagementService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ActiveUserFilter extends OncePerRequestFilter {
    private final UserManagementService userManagementService;

    public ActiveUserFilter(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken)) {
            String username = authentication.getName();
            if (!userManagementService.isUserActive(username)) {
                SecurityContextHolder.clearContext();
                response.sendRedirect(request.getContextPath() + "/access-denied");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/access-denied")
            || path.equals("/login")
            || path.equals("/logout")
            || path.startsWith("/css/")
            || path.startsWith("/error");
    }
}
