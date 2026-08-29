-- ===========================================
-- 智慧大棚AIoT系统 — 验收种子数据
-- ===========================================
-- 用途：系统集成验收所需的基础数据
-- 执行方式：docker compose exec mysql mysql -uroot -proot smart_greenhouse < init_seed_data.sql
-- ===========================================

-- 1. 用户数据（密码均为 BCrypt 加密的 "123456"）
INSERT INTO users (id, username, password, phone, real_name, role, status, created_at, updated_at)
VALUES
    (1, 'admin',   '$2b$10$hTjHXV3EIIEi1czZ.c2OGOxjDhD9lacQwa0L6MuUxyE5L2DWOS8Y.', '13800000001', '系统管理员', 'ADMIN',  true, NOW(), NOW()),
    (2, 'owner01', '$2b$10$hTjHXV3EIIEi1czZ.c2OGOxjDhD9lacQwa0L6MuUxyE5L2DWOS8Y.', '13800000002', '张棚主',     'OWNER',  true, NOW(), NOW()),
    (3, 'expert01','$2b$10$hTjHXV3EIIEi1czZ.c2OGOxjDhD9lacQwa0L6MuUxyE5L2DWOS8Y.', '13800000003', '李专家',     'EXPERT', true, NOW(), NOW()),
    (4, 'worker01','$2b$10$hTjHXV3EIIEi1czZ.c2OGOxjDhD9lacQwa0L6MuUxyE5L2DWOS8Y.', '13800000004', '王员工',     'WORKER',true, NOW(), NOW())
ON DUPLICATE KEY UPDATE username = VALUES(username);

-- 2. 大棚数据
INSERT INTO greenhouses (id, `name`, location, crop_type, owner_id, province, city, district, status, created_at, updated_at)
VALUES
    (1, '一号番茄大棚', '村东头', '番茄', 2, '河北省', '石家庄市', '藁城区', true, NOW(), NOW())
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- 3. 设备数据（与 Device Simulator devices.json 保持一致）
-- 注意：MySQL 中 name/description/last_value 是保留字，需反引号
INSERT INTO devices (id, `name`, device_sn, device_type, sensor_type, status, greenhouse_id, created_at, updated_at)
VALUES
    (1, '1号温度传感器',   'TEMP-001',  'SENSOR', 'TEMPERATURE',   'OFFLINE', 1, NOW(), NOW()),
    (2, '1号湿度传感器',   'HUM-001',   'SENSOR', 'HUMIDITY',      'OFFLINE', 1, NOW(), NOW()),
    (3, '1号光照传感器',   'LIGHT-001', 'SENSOR', 'LIGHT',         'OFFLINE', 1, NOW(), NOW()),
    (4, '1号CO2传感器',    'CO2-001',   'SENSOR', 'CO2',           'OFFLINE', 1, NOW(), NOW()),
    (5, '1号土壤湿度传感器','SOILM-001', 'SENSOR', 'SOIL_MOISTURE', 'OFFLINE', 1, NOW(), NOW()),
    (6, '1号土壤温度传感器','SOILT-001', 'SENSOR', 'SOIL_TEMP',    'OFFLINE', 1, NOW(), NOW()),
    -- 控制器设备（闭环五设备控制验证用）
    (7, '1号水泵控制器',   'PUMP-001',  'CONTROLLER', NULL,          'OFFLINE', 1, NOW(), NOW()),
    (8, '1号风机控制器',   'FAN-001',   'CONTROLLER', NULL,          'OFFLINE', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- 4. 预警规则（温度超过35°C触发告警）
INSERT INTO alert_rules (id, greenhouse_id, sensor_type, rule_type, condition_json, alert_level, enabled, created_at, updated_at)
VALUES
    (1, 1, 'TEMPERATURE', 'THRESHOLD', '{"min":10,"max":35}', 'WARNING', true, NOW(), NOW()),
    (2, 1, 'HUMIDITY',    'THRESHOLD', '{"min":30,"max":90}', 'WARNING', true, NOW(), NOW())
ON DUPLICATE KEY UPDATE sensor_type = VALUES(sensor_type);

-- 5. 知识库文档元数据（种子数据，内容后续通过 API 上传）
INSERT INTO knowledge_documents (id, title, category, file_type, file_size, chunk_count, vector_indexed, created_at, updated_at)
VALUES
    (1, '番茄种植技术指南',   '栽培技术',   'md', 2048, 0, false, NOW(), NOW()),
    (2, '常见病虫害防治手册', '病虫害防治', 'md', 4096, 0, false, NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title);

-- ---------- 8. 固件建档（与 tools/migrate_firmware_id.sql 一致）----------
-- 为所有尚未分配 firmware_id 的设备按 id 升序生成固件ID（00000001 起）并建档绑定
SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'devices' AND COLUMN_NAME = 'firmware_id'
);
SET @ddl = IF(@col_exists = 0,
  'ALTER TABLE devices ADD COLUMN firmware_id CHAR(8) NULL COMMENT ''固件ID(出厂预注册,全局唯一)'' AFTER device_sn',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'devices' AND INDEX_NAME = 'uk_devices_firmware_id'
);
SET @ddl2 = IF(@idx_exists = 0,
  'ALTER TABLE devices ADD UNIQUE INDEX uk_devices_firmware_id (firmware_id)',
  'SELECT 1');
PREPARE stmt2 FROM @ddl2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

CREATE TABLE IF NOT EXISTS firmwares (
  firmware_id varchar(8) NOT NULL COMMENT '固件ID(8位数字,全局唯一)',
  device_type varchar(20) NOT NULL COMMENT '设备类型 SENSOR/CONTROLLER',
  sensor_type varchar(30) DEFAULT NULL COMMENT '传感器子类型',
  firmware_version varchar(20) DEFAULT NULL COMMENT '固件版本',
  batch_no varchar(30) DEFAULT NULL COMMENT '出厂批次号',
  status varchar(10) NOT NULL DEFAULT 'UNBOUND' COMMENT 'UNBOUND未绑定/BOUND已绑定',
  bound_device_id bigint DEFAULT NULL COMMENT '绑定设备ID(devices.id)',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (firmware_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='固件档案表(出厂预注册)';

INSERT INTO firmwares (firmware_id, device_type, sensor_type, firmware_version, batch_no, status, bound_device_id, created_at)
SELECT
  LPAD(@rn := @rn + 1, 8, '0'),
  d.device_type,
  d.sensor_type,
  '1.0.0',
  'SEED-20260829',
  'BOUND',
  d.id,
  NOW()
FROM devices d
CROSS JOIN (SELECT @rn := (SELECT COALESCE(MAX(CAST(firmware_id AS UNSIGNED)), 0)
                           FROM firmwares WHERE firmware_id REGEXP '^[0-9]+$')) r
WHERE d.firmware_id IS NULL
ORDER BY d.id;

UPDATE devices d
JOIN firmwares f ON f.bound_device_id = d.id
SET d.firmware_id = f.firmware_id
WHERE d.firmware_id IS NULL;
