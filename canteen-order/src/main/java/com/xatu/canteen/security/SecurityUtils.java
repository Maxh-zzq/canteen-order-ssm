package com.xatu.canteen.security;

import com.xatu.canteen.common.BizException;
import com.xatu.canteen.common.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户工具，业务层统一从这里取用户，避免各处解析 Token。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser currentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser)) {
            return null;
        }
        return (LoginUser) authentication.getPrincipal();
    }

    public static LoginUser currentUser() {
        LoginUser user = currentUserOrNull();
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }

    public static Long currentUserId() {
        return currentUser().getId();
    }

    public static Long currentUserIdOrNull() {
        LoginUser user = currentUserOrNull();
        return user == null ? null : user.getId();
    }

    public static boolean isAdmin() {
        LoginUser user = currentUserOrNull();
        return user != null && "ADMIN".equals(user.getRole());
    }
}
