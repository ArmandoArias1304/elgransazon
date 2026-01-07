CREATE TABLE IF NOT EXISTS daily_order_counters (
    counter_date DATE PRIMARY KEY,
    last_sequence INT NOT NULL DEFAULT 0
);
