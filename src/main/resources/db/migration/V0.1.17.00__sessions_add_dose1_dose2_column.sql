SET
    SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET
    time_zone = "+00:00";

ALTER TABLE `sessions`
    ADD COLUMN IF NOT EXISTS `available_capacity_dose1` INT DEFAULT NULL AFTER `available_capacity`,
    ADD COLUMN IF NOT EXISTS `available_capacity_dose2` INT DEFAULT NULL AFTER `available_capacity_dose1`;
