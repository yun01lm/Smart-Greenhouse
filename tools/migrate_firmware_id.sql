-- ============================================================
-- 存量设备固件ID迁移脚本（选B方案：一次性全量迁移）
-- ============================================================
-- 说明：
--   1. devices 表新增 firmware_id 列（8位数字，全局唯一）+ 唯一索引
--   2. 新建 firmwares 固件档案表（与 Firmware 实体映射一致）
--   3. 为存量设备按 id 升序生成固件ID（00000001 起），建档并绑定
--
-- 幂等性：可重复执行（列存在则跳过；已建档的固件不重复生成）
-- 执行方式: docker exec -i greenhouse-mysql mysql -uroot -proot smart_greenhouse < tools/migrate_firmware_id.sql
-- ============================================================

-- 1. devices 表加列（MySQL 不支持 ADD COLUMN IF NOT EXISTS，先判断）
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

-- 2. devices.firmware_id 唯一索引（存在则跳过）
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

-- 3. 新建 firmwares 固件档案表
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

-- 4. 为存量设备建档（按 id 升序生成 00000001 起，跳过已建档的）
INSERT INTO firmwares (firmware_id, device_type, sensor_type, firmware_version, batch_no, status, bound_device_id, created_at)
SELECT
  LPAD(@rn := @rn + 1, 8, '0'),
  d.device_type,
  d.sensor_type,
  '1.0.0',
  'MIGRATE-20260829',
  'BOUND',
  d.id,
  NOW()
FROM devices d
CROSS JOIN (SELECT @rn := (SELECT COALESCE(MAX(CAST(firmware_id AS UNSIGNED)), 0)
                           FROM firmwares WHERE firmware_id REGEXP '^[0-9]+$')) r
WHERE d.firmware_id IS NULL
ORDER BY d.id;

-- 5. 回填 devices.firmware_id（只回填尚未有 firmware_id 的存量设备）
UPDATE devices d
JOIN firmwares f ON f.bound_device_id = d.id
SET d.firmware_id = f.firmware_id
WHERE d.firmware_id IS NULL;

-- 6. 校验结果
SELECT 'devices 总数' AS 项目, COUNT(*) AS 数值 FROM devices
UNION ALL SELECT '已分配 firmware_id 的设备', COUNT(*) FROM devices WHERE firmware_id IS NOT NULL
UNION ALL SELECT 'firmwares 档案总数', COUNT(*) FROM firmwares
UNION ALL SELECT '已绑定固件数', COUNT(*) FROM firmwares WHERE status = 'BOUND';
