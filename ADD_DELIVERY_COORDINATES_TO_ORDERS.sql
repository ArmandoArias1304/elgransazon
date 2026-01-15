-- Script to add GPS coordinates for delivery orders
-- Run this script to enable "View on Google Maps" feature for delivery personnel

-- Add latitude and longitude columns to orders table
ALTER TABLE orders 
ADD COLUMN delivery_latitude DOUBLE NULL,
ADD COLUMN delivery_longitude DOUBLE NULL;

-- Verify the columns were added
DESCRIBE orders;

-- Note: New orders with addresses from customer_addresses will automatically have coordinates
-- Existing orders will have NULL coordinates and won't show the "Ver ubicación en Google Maps" button
