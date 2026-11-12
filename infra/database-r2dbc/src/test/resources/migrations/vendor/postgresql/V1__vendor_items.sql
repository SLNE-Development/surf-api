CREATE TABLE IF NOT EXISTS vendor_items (
    id     INT         GENERATED ALWAYS AS IDENTITY,
    vendor VARCHAR(16) NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO vendor_items (vendor)
VALUES ('postgresql');
