-- noinspection SqlResolveForFile

INSERT INTO vaccine_centers (id, address, district_name, fee_type, name, pincode, state_name)
VALUES (383358, NULL, 'Madhepura', 'Free', 'Gamhariya PHC', '852108', 'Bihar'),
       (3, NULL, 'Central Delhi', 'Free', 'Aruna Asaf Ali Hospital DH', '110054', 'Delhi'),
       (168, NULL, 'Kaithal', 'Free', 'PHC Batta Covishield 18-45', '136117', 'Haryana'),
       (190, NULL, 'Kaithal', 'Free', 'SC Bhunsla Covishield Above 45', '136034', 'Haryana'),
       (196, NULL, 'Kaithal', 'Free', 'SC Rewar Jagir', '136034', 'Haryana'),
       (259, NULL, 'Kaithal', 'Free', 'SC Chaba', '136034', 'Haryana'),
       (276, NULL, 'Kaithal', 'Free', 'PHC Habri Covishield 18-45', '136026', 'Haryana'),
       (280, NULL, 'Kaithal', 'Free', 'SC Deeg', '136026', 'Haryana'),
       (328, NULL, 'Kaithal', 'Free', 'SC Kheri Gulam Ali Above 45', '136035', 'Haryana'),
       (436, NULL, 'Kaithal', 'Free', 'SC Kakaut', '136027', 'Haryana');

INSERT INTO sessions (id, available_capacity, available_capacity_dose1,
                      available_capacity_dose2, date, min_age_limit, vaccine,
                      processed_at)
VALUES ('001813bc-1607-42d9-9ef6-e58ba4e42d1d', 98, 48, 50, '23-05-2021', 45, 'COVISHIELD', NULL);

INSERT INTO vaccine_centers_sessions (center_entity_id, sessions_id)
VALUES (383358, '001813bc-1607-42d9-9ef6-e58ba4e42d1d');

INSERT INTO states(id, state_name)
VALUES (12, 'Haryana');

INSERT INTO districts(id, district_name, state_id)
VALUES (201, 'Charkhi Dadri', 12);

INSERT INTO pincodes (id, pincode, district_id)
VALUES ('05c1ed93-ae15-11eb-9793-7a9bcab32cce', '127310', 201);

COMMIT;
