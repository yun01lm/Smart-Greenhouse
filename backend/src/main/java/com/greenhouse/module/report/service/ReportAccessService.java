package com.greenhouse.module.report.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.Greenhouse;
import com.greenhouse.entity.User;
import com.greenhouse.repository.EmployeePermissionRepository;
import com.greenhouse.repository.GreenhouseRepository;
import com.greenhouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 报表导出访问控制服务（R8 新增）
 * <p>
 * 数据导出仅开放给棚主(OWNER)及其名下技术员(WORKER)：
 * - OWNER：只能导出自己名下的大棚数据；
 * - WORKER：只能导出被授权管理的大棚数据；
 * - ADMIN / EXPERT：无导出权限（管理员的系统级报表走 /api/v1/admin/report，不暴露给前端）。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ReportAccessService {

    private final UserRepository userRepository;
    private final GreenhouseRepository greenhouseRepository;
    private final EmployeePermissionRepository permissionRepository;

    /**
     * 校验导出权限，无权限时抛出 GREENHOUSE_ACCESS_DENIED
     */
    public void assertExportAccess(Long userId, Long greenhouseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.EXPERT) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        Greenhouse greenhouse = greenhouseRepository.findById(greenhouseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));
        boolean allowed = (user.getRole() == User.Role.OWNER && greenhouse.getOwnerId().equals(userId))
                || (user.getRole() == User.Role.WORKER
                    && permissionRepository.existsByEmployeeIdAndGreenhouseId(userId, greenhouseId));
        if (!allowed) {
            throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
        }
    }
}