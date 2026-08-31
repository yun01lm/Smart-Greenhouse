package com.greenhouse.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 地区名称规范化（方案二：手填 + 强制对齐格式）
 * <p>
 * 内置全国省/市/县三级标准名表（region_standard.json，来自国家统计局行政区划数据），
 * 保存用户/大棚地区时自动把手填值对齐为标准名：
 * 「河北」→「河北省」、「衡水」→「衡水市」、「深州」→「深州市」、
 * 「北京」→「北京市」、「广西」→「广西壮族自治区」。
 * <p>
 * 匹配规则：手填值去掉后缀（省/市/区/县/自治区/自治州/地区/盟/旗…）后与标准名/官方简称比对，
 * 命中则返回标准名，未命中原样保留（避免误改自定义地名）。
 * 乡镇/村两级不强制（数据量 4 万+ 且村级多为俗称），保留自由填写。
 */
@Slf4j
@Component
public class RegionNormalizer {

    /** 省 → 标准名 */
    private final Map<String, String> provinceMap = new HashMap<>();
    /** 市 → 标准名 */
    private final Map<String, String> cityMap = new HashMap<>();
    /** 县/区 → 标准名 */
    private final Map<String, String> districtMap = new HashMap<>();

    /** 常见行政区划后缀（按长度降序，先匹配长的，如「自治区」优先于「区」） */
    private static final String[] SUFFIXES = {
            "特别行政区", "壮族自治区", "回族自治区", "维吾尔自治区", "自治区",
            "自治州", "自治县", "自治旗", "地区", "林区", "矿区",
            "省", "市", "区", "县", "盟", "旗"
    };

    @PostConstruct
    public void init() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ClassPathResource res = new ClassPathResource("region_standard.json");
            try (InputStream in = res.getInputStream()) {
                Map<String, List<Map<String, Object>>> root = mapper.readValue(
                        in, new TypeReference<Map<String, List<Map<String, Object>>>>() {});
                fill(root.get("provinces"), provinceMap);
                fill(root.get("cities"), cityMap);
                fill(root.get("districts"), districtMap);
            }
            log.info("地区标准名表加载完成: 省{} 市{} 县{}", provinceMap.size(), cityMap.size(), districtMap.size());
        } catch (Exception e) {
            log.error("地区标准名表加载失败，地区规范化将不可用", e);
        }
    }

    private void fill(List<Map<String, Object>> items, Map<String, String> target) {
        if (items == null) {
            return;
        }
        for (Map<String, Object> item : items) {
            String name = String.valueOf(item.get("name"));
            target.put(name, name);
            Object aliases = item.get("aliases");
            if (aliases instanceof List<?> list) {
                for (Object a : list) {
                    target.put(String.valueOf(a), name);
                }
            }
        }
    }

    /**
     * 规范化省级：手填「河北」→「河北省」
     */
    public String normalizeProvince(String input) {
        return normalize(input, provinceMap);
    }

    /**
     * 规范化市级：手填「衡水」→「衡水市」
     * <p>直辖市（北京/天津/上海/重庆）无地级市，市级字段实际填的是区，
     * 按区级标准名对齐（「朝阳」→「朝阳区」而非「朝阳市」）。</p>
     */
    public String normalizeCity(String province, String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        String p = province == null ? "" : province.trim();
        boolean isMunicipality = "北京市".equals(p) || "天津市".equals(p)
                || "上海市".equals(p) || "重庆市".equals(p);
        return normalize(input, isMunicipality ? districtMap : cityMap);
    }

    /**
     * 规范化市级（无省级上下文时的降级入口）
     */
    public String normalizeCity(String input) {
        return normalize(input, cityMap);
    }

    /**
     * 规范化县/区级：手填「深州」→「深州市」、「正定」→「正定县」
     */
    public String normalizeDistrict(String input) {
        return normalize(input, districtMap);
    }

    private String normalize(String input, Map<String, String> map) {
        if (input == null || input.isBlank()) {
            return input;
        }
        String trimmed = input.trim();
        // 1) 直接命中（含别名表，如「河北」）→ 返回标准名
        String exact = map.get(trimmed);
        if (exact != null) {
            return exact;
        }
        // 2) 去掉后缀后命中（如「河北市」「衡水市」→ 去「市」→ 命中）
        String stripped = stripSuffix(trimmed);
        if (!stripped.isEmpty() && !stripped.equals(trimmed)) {
            String hit = map.get(stripped);
            if (hit != null) {
                return hit;
            }
        }
        // 3) 未命中 → 原样保留（可能是自定义地名，不误改）
        return trimmed;
    }

    private String stripSuffix(String s) {
        for (String suffix : SUFFIXES) {
            if (s.length() > suffix.length() && s.endsWith(suffix)) {
                return s.substring(0, s.length() - suffix.length());
            }
        }
        return s;
    }

    /** 供批量清洗存量数据使用：暴露映射表（只读视图） */
    public Map<String, String> provinceMap() {
        return java.util.Collections.unmodifiableMap(provinceMap);
    }

    public Map<String, String> cityMap() {
        return java.util.Collections.unmodifiableMap(cityMap);
    }

    public Map<String, String> districtMap() {
        return java.util.Collections.unmodifiableMap(districtMap);
    }
}
