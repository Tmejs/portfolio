-- Set search path for this migration
SET search_path TO banking;

-- Add balance_before column to transactions table
ALTER TABLE banking.transactions 
ADD COLUMN balance_before DECIMAL(19,2) NOT NULL DEFAULT 0.00;

-- Update the column to not have a default value after adding it
-- (This allows existing data to have a default value during migration)
ALTER TABLE banking.transactions 
ALTER COLUMN balance_before DROP DEFAULT;
