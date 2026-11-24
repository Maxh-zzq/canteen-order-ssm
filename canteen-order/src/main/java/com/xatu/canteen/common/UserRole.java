package com.xatu.canteen.common;

import lombok.Getter;

/**
 * 用户角色。系统中只有学生、档口员工、管理员三类。
 */
@Getter
public enum UserRole {

    STUDENT("STUDENT", "学生"),
    STAFF("STAFF", "档口员工"),
    ADMIN("ADMIN", "管理员");

    private final String code;
    private final String desc;

    UserRole(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static UserRole of(String code) {
        for (UserRole role : values()) {
            if (role.code.equalsIgnoreCase(code)) {
                return role;
            }
        }
        return STUDENT;
    }
}
