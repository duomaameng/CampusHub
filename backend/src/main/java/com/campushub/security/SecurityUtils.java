package com.campushub.security;

import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<Long> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CampusHubUserDetails userDetails) {
            return Optional.of(userDetails.getUserId());
        }
        return Optional.empty();
    }

    public static Long requireCurrentUserId() {
        return getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    public static Optional<String> getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CampusHubUserDetails userDetails) {
            return userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(Object::toString)
                    .map(role -> role.replace("ROLE_", ""));
        }
        return Optional.empty();
    }

    public static boolean isAdmin() {
        return getCurrentUserRole().map("ADMIN"::equals).orElse(false);
    }

    public static boolean isCurrentUser(Long userId) {
        return getCurrentUserId().map(id -> id.equals(userId)).orElse(false);
    }
}
