SET
SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET
time_zone = "+00:00";

--
-- Table structure for table `user_notifications`
--

CREATE TABLE `user_notifications`
(
    `user_id`           varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `pincode`           varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `notification_hash` varchar(255) COLLATE utf8mb4_unicode_ci,
    `notified_at`       timestamp,
    PRIMARY KEY (`user_id`, `pincode`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
