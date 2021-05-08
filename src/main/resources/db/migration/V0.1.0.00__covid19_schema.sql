SET
    SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET
    time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT = @@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS = @@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION = @@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `covid19`
--

-- --------------------------------------------------------

--
-- Table structure for table `districts`
--

CREATE TABLE `districts`
(
    `id`            int(11)                                 NOT NULL,
    `district_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
    `state_id`      int(11)                                 NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `pincodes`
--

CREATE TABLE `pincodes`
(
    `id`          varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
    `pincode`     varchar(6) COLLATE utf8mb4_unicode_ci  NOT NULL,
    `district_id` int(11)                                NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `states`
--

CREATE TABLE `states`
(
    `id`         int(11)                                 NOT NULL,
    `state_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `districts`
--
ALTER TABLE `districts`
    ADD PRIMARY KEY (`id`),
    ADD KEY `state_id` (`state_id`);

--
-- Indexes for table `pincodes`
--
ALTER TABLE `pincodes`
    ADD PRIMARY KEY (`id`),
    ADD KEY `pincode` (`pincode`),
    ADD KEY `district_id` (`district_id`);

--
-- Indexes for table `states`
--
ALTER TABLE `states`
    ADD PRIMARY KEY (`id`);

--
-- Constraints for dumped tables
--

--
-- Constraints for table `districts`
--
ALTER TABLE `districts`
    ADD CONSTRAINT `districts_ibfk_1` FOREIGN KEY (`state_id`) REFERENCES `states` (`id`);

--
-- Constraints for table `pincodes`
--
ALTER TABLE `pincodes`
    ADD CONSTRAINT `pincodes_ibfk_1` FOREIGN KEY (`district_id`) REFERENCES `districts` (`id`);

--
-- Table structure for table `sessions`
--

CREATE TABLE `sessions`
(
    `id`                 varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `available_capacity` int(11)                                 DEFAULT NULL,
    `date`               varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `min_age_limit`      int(11)                                 DEFAULT NULL,
    `vaccine`            varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `vaccine_centers`
--

CREATE TABLE `vaccine_centers`
(
    `id`            bigint(20) NOT NULL,
    `address`       varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `district_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `fee_type`      varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `name`          varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `pincode`       varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `state_name`    varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `vaccine_centers_sessions`
--

CREATE TABLE `vaccine_centers_sessions`
(
    `center_entity_id` bigint(20)                              NOT NULL,
    `sessions_id`      varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `sessions`
--
ALTER TABLE `sessions`
    ADD PRIMARY KEY (`id`);

--
-- Indexes for table `vaccine_centers`
--
ALTER TABLE `vaccine_centers`
    ADD PRIMARY KEY (`id`);

--
-- Indexes for table `vaccine_centers_sessions`
--
ALTER TABLE `vaccine_centers_sessions`
    ADD PRIMARY KEY (`center_entity_id`, `sessions_id`),
    ADD UNIQUE KEY `UK_8cxlr15ohx4l6s01bgh1otlpe` (`sessions_id`);

--
-- Constraints for dumped tables
--

--
-- Constraints for table `vaccine_centers_sessions`
--
ALTER TABLE `vaccine_centers_sessions`
    ADD CONSTRAINT `FK9xhiy5riep1lknwgrjucqj714` FOREIGN KEY (`center_entity_id`) REFERENCES `vaccine_centers` (`id`),
    ADD CONSTRAINT `FKk9yumo7p4xf24nji4r7i92dyr` FOREIGN KEY (`sessions_id`) REFERENCES `sessions` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT = @OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS = @OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION = @OLD_COLLATION_CONNECTION */;
