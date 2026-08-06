package com.greenhouse.common;

/**
 * 密码策略工具（R16：统一初始密码与改密规则）
 * <p>
 * 规则与 APP 注册接口保持一致：长度>=8，且必须包含字母和数字。
 * </p>
 */
public final class PasswordPolicy {

    /** 管理员创建用户时统一设置的初始密码 */
    public static final String INITIAL_PASSWORD = "123456";

    private PasswordPolicy() {
    }

    /**
     * 校验密码复杂度，不满足时抛出 BusinessException
     */
    public static void validate(String password) {
        if (password == null || password.length() < 8
                || !password.matches(".*[a-zA-Z].*") || !password.matches(".*[0-9].*")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码至少8位，且必须包含字母和数字");
        }
    }
}