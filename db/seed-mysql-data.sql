INSERT INTO `labsheartbeatbackend_event` (`event_key`, `event_timestamp`, `event_value`)
VALUES ('test-dataset-3', NOW() - INTERVAL 2 DAY + INTERVAL FLOOR(RAND() * 2000000) SECOND, FLOOR(RAND() * 100));


SELECT COUNT(*) FROM labsheartbeatbackend_event;
