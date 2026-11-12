CREATE TABLE IF NOT EXISTS vendor_items (
    id     INT         NOT NULL AUTO_INCREMENT,
    vendor VARCHAR(16) NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO vendor_items (vendor)
VALUES ('mariadb');
