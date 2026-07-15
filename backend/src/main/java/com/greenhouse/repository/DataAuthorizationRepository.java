package com.greenhouse.repository;

import com.greenhouse.entity.DataAuthorization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 数据授权 Repository
 */
@Repository
public interface DataAuthorizationRepository extends JpaRepository<DataAuthorization, Long> {

    /**
     * 查询专家对某用户某大棚的有效授权
     */
    Optional<DataAuthorization> findTopByExpertIdAndUserIdAndGreenhouseIdAndStatusOrderByRequestedAtDesc(
            Long expertId, Long userId, Long greenhouseId, DataAuthorization.AuthorizationStatus status);

    /**
     * 查询用户待处理的授权请求
     */
    List<DataAuthorization> findByUserIdAndStatusOrderByRequestedAtDesc(Long userId, DataAuthorization.AuthorizationStatus status);

    /**
     * 查询用户的有效授权
     */
    List<DataAuthorization> findByUserIdAndStatusOrderByApprovedAtDesc(Long userId, DataAuthorization.AuthorizationStatus status);

    /**
     * 查询专家的有效授权
     */
    List<DataAuthorization> findByExpertIdAndStatusOrderByApprovedAtDesc(Long expertId, DataAuthorization.AuthorizationStatus status);

    /**
     * 查询用户的授权历史
     */
    List<DataAuthorization> findByUserIdOrderByRequestedAtDesc(Long userId);

    /** ADMIN 全量按状态分页查询 */
    Page<DataAuthorization> findByStatus(DataAuthorization.AuthorizationStatus status, Pageable pageable);
}
