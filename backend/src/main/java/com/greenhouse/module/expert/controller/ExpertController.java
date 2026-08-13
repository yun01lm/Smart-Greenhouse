package com.greenhouse.module.expert.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.module.expert.service.ExpertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 专家列表 API
 * <p>
 * 路径前缀：/api/v1/experts
 * </p>
 */
@RestController
@RequestMapping("/api/v1/experts")
@RequiredArgsConstructor
public class ExpertController {

    private final ExpertService expertService;

    /**
     * 专家列表
     * GET /api/v1/experts?specialty=蔬菜植保&onlineOnly=true&page=0&size=10
     */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) Boolean onlineOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<Map<String, Object>> experts = expertService.getExpertList(specialty, onlineOnly, page, size);
        return ApiResponse.success(experts);
    }
}
