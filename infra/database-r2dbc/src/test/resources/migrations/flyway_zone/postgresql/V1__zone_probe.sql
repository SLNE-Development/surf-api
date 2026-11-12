CREATE TABLE zone_probe (
    id           INT         NOT NULL,
    source       VARCHAR(8)  NOT NULL,
    session_zone VARCHAR(64) NOT NULL,
    wall_clock   TIMESTAMP   NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO zone_probe (id, source, session_zone, wall_clock)
VALUES (1, 'sql', current_setting('TimeZone'), LOCALTIMESTAMP);
