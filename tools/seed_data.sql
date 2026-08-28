-- ============================================================
-- 智慧大棚 AIoT — 数据清理与种子数据填充脚本
-- 执行方式: docker exec -i greenhouse-mysql mysql -uroot -proot
--           --default-character-set=utf8mb4 smart_greenhouse < 本文件
-- ============================================================

USE smart_greenhouse;

-- ---------- 1. 清理脏数据 ----------
-- 1.1 用户真实姓名被存为问号的账号（test_user_082 / tech01 / worker02）
DELETE FROM employee_permissions WHERE employee_id IN (11, 12);
DELETE FROM users WHERE id IN (5, 11, 12);

-- 1.2 QA 历史记录中问题被存为问号（编码损坏）的记录
DELETE FROM qa_records WHERE question LIKE '%?%';

-- 1.3 专家聊天里的测试垃圾消息（乱敲字符/乱码）
DELETE FROM chat_messages WHERE id IN (9, 11, 13, 14);

-- ---------- 2. 重建技术员 + 新增员工（归属棚主 owner01，密码 123456）----------
-- 密码哈希与 owner01/worker01 相同（BCrypt, 123456）
INSERT INTO users (username, password, phone, real_name, role, owner_id, status, created_at, updated_at) VALUES
('tech01',   '$2b$10$hTjHXV3EIIEi1czZ.c2OGOxjDhD9lacQwa0L6MuUxyE5L2DWOS8Y.', '13800009991', '陈技工', 'TECHNICIAN', 2, 1, NOW(), NOW()),
('worker03', '$2b$10$hTjHXV3EIIEi1czZ.c2OGOxjDhD9lacQwa0L6MuUxyE5L2DWOS8Y.', '13900000103', '赵小强', 'WORKER', 2, 1, NOW(), NOW()),
('worker04', '$2b$10$hTjHXV3EIIEi1czZ.c2OGOxjDhD9lacQwa0L6MuUxyE5L2DWOS8Y.', '13900000104', '刘大柱', 'WORKER', 2, 1, NOW(), NOW()),
('worker05', '$2b$10$hTjHXV3EIIEi1czZ.c2OGOxjDhD9lacQwa0L6MuUxyE5L2DWOS8Y.', '13900000105', '孙秀兰', 'WORKER', 2, 1, NOW(), NOW());

-- ---------- 3. 新增专家（2 名）----------
INSERT INTO users (username, password, phone, real_name, role, expert_specialty, expert_status, status, created_at, updated_at) VALUES
('expert02', '$2b$10$hTjHXV3EIIEi1czZ.c2OGOxjDhD9lacQwa0L6MuUxyE5L2DWOS8Y.', '13900000202', '王农艺', 'EXPERT', '蔬菜栽培与设施农业', 'ONLINE', 1, NOW(), NOW()),
('expert03', '$2b$10$hTjHXV3EIIEi1czZ.c2OGOxjDhD9lacQwa0L6MuUxyE5L2DWOS8Y.', '13900000203', '赵植保', 'EXPERT', '植物保护与病虫害防治', 'ONLINE', 1, NOW(), NOW());

-- ---------- 4. 新增棚主 owner02 + 黄瓜大棚 ----------
INSERT INTO users (username, password, phone, real_name, role, status, created_at, updated_at) VALUES
('owner02', '$2b$10$hTjHXV3EIIEi1czZ.c2OGOxjDhD9lacQwa0L6MuUxyE5L2DWOS8Y.', '13900000210', '李建国', 'OWNER', 1, NOW(), NOW());

INSERT INTO greenhouses (name, location, crop_type, owner_id, province, city, district, town, village, status, created_at) 
SELECT '黄瓜种植大棚', '河北省石家庄市正定县南牛镇曹村', '黄瓜', id, '河北省', '石家庄市', '正定县', '南牛镇', '曹村', 1, NOW()
FROM users WHERE username = 'owner02';

-- ---------- 5. 员工权限（员工挂到大棚 1-5）----------
-- 技术员：全部权限
INSERT INTO employee_permissions (employee_id, owner_id, greenhouse_id, can_view_data, can_view_history, can_control_device, can_view_alerts, can_ask_expert, can_diagnose, created_at, updated_at)
SELECT u.id, 2, g.id, 1, 1, 1, 1, 1, 1, NOW(), NOW()
FROM users u CROSS JOIN greenhouses g
WHERE u.username = 'tech01' AND g.id BETWEEN 1 AND 5;

-- 普通员工：查看数据/控制设备/查看预警
INSERT INTO employee_permissions (employee_id, owner_id, greenhouse_id, can_view_data, can_view_history, can_control_device, can_view_alerts, can_ask_expert, can_diagnose, created_at, updated_at)
SELECT u.id, 2, g.id, 1, 0, 1, 1, 0, 0, NOW(), NOW()
FROM users u CROSS JOIN greenhouses g
WHERE u.username IN ('worker03', 'worker04', 'worker05') AND g.id BETWEEN 1 AND 5;

-- ---------- 6. 为大棚 2-6 填充设备（每棚 4 传感器 + 2 控制器）----------
INSERT INTO devices (name, device_sn, device_type, sensor_type, status, greenhouse_id, install_location, description, created_at, updated_at)
SELECT '1号温度传感器', d.sn, 'SENSOR', 'TEMPERATURE', 'OFFLINE', gh.id, d.loc, '环境监测', NOW(), NOW()
FROM greenhouses gh JOIN (
  SELECT 2 AS gid, 'TEMP-101' AS sn, '大棚东侧' AS loc UNION ALL
  SELECT 3, 'TEMP-201', '大棚东侧' UNION ALL
  SELECT 4, 'TEMP-301', '大棚东侧' UNION ALL
  SELECT 5, 'TEMP-401', '大棚东侧' UNION ALL
  SELECT 6, 'TEMP-501', '大棚东侧'
) d ON gh.id = d.gid;

INSERT INTO devices (name, device_sn, device_type, sensor_type, status, greenhouse_id, install_location, description, created_at, updated_at)
SELECT '1号湿度传感器', d.sn, 'SENSOR', 'HUMIDITY', 'OFFLINE', gh.id, d.loc, '环境监测', NOW(), NOW()
FROM greenhouses gh JOIN (
  SELECT 2 AS gid, 'HUM-101' AS sn, '大棚东侧' AS loc UNION ALL
  SELECT 3, 'HUM-201', '大棚东侧' UNION ALL
  SELECT 4, 'HUM-301', '大棚东侧' UNION ALL
  SELECT 5, 'HUM-401', '大棚东侧' UNION ALL
  SELECT 6, 'HUM-501', '大棚东侧'
) d ON gh.id = d.gid;

INSERT INTO devices (name, device_sn, device_type, sensor_type, status, greenhouse_id, install_location, description, created_at, updated_at)
SELECT '1号CO2传感器', d.sn, 'SENSOR', 'CO2', 'OFFLINE', gh.id, d.loc, '环境监测', NOW(), NOW()
FROM greenhouses gh JOIN (
  SELECT 2 AS gid, 'CO2-101' AS sn, '大棚中部' AS loc UNION ALL
  SELECT 3, 'CO2-201', '大棚中部' UNION ALL
  SELECT 4, 'CO2-301', '大棚中部' UNION ALL
  SELECT 5, 'CO2-401', '大棚中部' UNION ALL
  SELECT 6, 'CO2-501', '大棚中部'
) d ON gh.id = d.gid;

INSERT INTO devices (name, device_sn, device_type, sensor_type, status, greenhouse_id, install_location, description, created_at, updated_at)
SELECT '1号光照传感器', d.sn, 'SENSOR', 'LIGHT', 'OFFLINE', gh.id, d.loc, '环境监测', NOW(), NOW()
FROM greenhouses gh JOIN (
  SELECT 2 AS gid, 'LIGHT-101' AS sn, '大棚中部' AS loc UNION ALL
  SELECT 3, 'LIGHT-201', '大棚中部' UNION ALL
  SELECT 4, 'LIGHT-301', '大棚中部' UNION ALL
  SELECT 5, 'LIGHT-401', '大棚中部' UNION ALL
  SELECT 6, 'LIGHT-501', '大棚中部'
) d ON gh.id = d.gid;

INSERT INTO devices (name, device_sn, device_type, sensor_type, status, greenhouse_id, install_location, description, created_at, updated_at)
SELECT '1号水泵控制器', d.sn, 'CONTROLLER', NULL, 'OFFLINE', gh.id, d.loc, '灌溉控制', NOW(), NOW()
FROM greenhouses gh JOIN (
  SELECT 2 AS gid, 'PUMP-101' AS sn, '大棚西侧' AS loc UNION ALL
  SELECT 3, 'PUMP-201', '大棚西侧' UNION ALL
  SELECT 4, 'PUMP-301', '大棚西侧' UNION ALL
  SELECT 5, 'PUMP-401', '大棚西侧' UNION ALL
  SELECT 6, 'PUMP-501', '大棚西侧'
) d ON gh.id = d.gid;

INSERT INTO devices (name, device_sn, device_type, sensor_type, status, greenhouse_id, install_location, description, created_at, updated_at)
SELECT '1号风机控制器', d.sn, 'CONTROLLER', NULL, 'OFFLINE', gh.id, d.loc, '通风控制', NOW(), NOW()
FROM greenhouses gh JOIN (
  SELECT 2 AS gid, 'FAN-101' AS sn, '大棚西侧' AS loc UNION ALL
  SELECT 3, 'FAN-201', '大棚西侧' UNION ALL
  SELECT 4, 'FAN-301', '大棚西侧' UNION ALL
  SELECT 5, 'FAN-401', '大棚西侧' UNION ALL
  SELECT 6, 'FAN-501', '大棚西侧'
) d ON gh.id = d.gid;

-- ---------- 7. 重新编写几条 QA 历史记录（干净的中文问答）----------
INSERT INTO qa_records (question, answer, sources, user_id, input_type, created_at) VALUES
('番茄叶子发黄是什么原因，怎么处理？', '番茄叶子发黄常见原因有四类：1) 浇水不当——水大沤根或干旱缺水；2) 温度不适——夜间低于10℃或连续阴天后暴晒；3) 缺素——缺氮老叶先黄、缺铁新叶黄白；4) 病害——早疫病、叶霉病等。处理上先控水控温，再按症状补肥或用药，严重时拍照联系专家诊断。', '[{"title": "番茄种植技术指南", "category": "栽培技术"}, {"title": "常见病虫害防治手册", "category": "病虫害防治"}]', 1, 'TEXT', NOW() - INTERVAL 2 DAY),
('大棚黄瓜霜霉病怎么防治？', '黄瓜霜霉病防治要点：1) 控制棚内湿度，加强通风，避免夜间叶面结露；2) 发病初期摘除病叶带出棚外；3) 药剂防治可选烯酰吗啉、霜脲氰等，注意轮换用药；4) 浇水选择晴天上午，避免大水漫灌。', '[{"title": "常见病虫害防治手册", "category": "病虫害防治"}, {"title": "番茄种植技术指南", "category": "栽培技术"}]', 1, 'TEXT', NOW() - INTERVAL 1 DAY),
('番茄定植密度多少合适？', '番茄定植密度依品种和整枝方式而定：大果型单干整枝一般每亩2500-3000株，株距35-40厘米、行距60-70厘米；樱桃番茄可适当密植。设施大棚内还要考虑通风透光，密度过大会增加灰霉病等病害风险。', '[{"title": "番茄种植技术指南", "category": "栽培技术"}]', 1, 'TEXT', NOW() - INTERVAL 6 HOUR);
