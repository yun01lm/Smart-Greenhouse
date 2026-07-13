package com.greenhouse.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户认证服务（临时实现）
 * <p>
 * 步骤3使用内存用户，步骤4（C1用户认证模块）会改为数据库查询。
 * </p>
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    /**
     * 临时内存用户存储
     * TODO: 步骤4改为从数据库查询
     */
    private final Map<String, UserDetails> tempUsers = new ConcurrentHashMap<>();

    public UserDetailsServiceImpl() {
        // 预置一个管理员账号用于开发测试
        tempUsers.put("admin", User.builder()
                .username("admin")
                .password("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh") // 实际是 admin123 的BCrypt
                .roles("ADMIN")
                .build());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails user = tempUsers.get(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        return user;
    }

    /**
     * 临时添加用户（步骤4会删除此方法，改用数据库）
     */
    public void addTempUser(String username, String password, String role) {
        tempUsers.put(username, User.builder()
                .username(username)
                .password(password)
                .roles(role)
                .build());
    }
}
