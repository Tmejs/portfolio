-- Create banking schema
CREATE SCHEMA IF NOT EXISTS banking;

-- Set search path for this migration
SET search_path TO banking;

-- Create accounts table in banking schema
CREATE TABLE banking.accounts (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    customer_name VARCHAR(100) NOT NULL,
    customer_email VARCHAR(150) NOT NULL,
    account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('CHECKING', 'SAVINGS')),
    balance DECIMAL(19,2) NOT NULL DEFAULT 0.00 CHECK (balance >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'CLOSED', 'FROZEN')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE UNIQUE INDEX idx_accounts_number ON banking.accounts (account_number);
CREATE INDEX idx_accounts_customer_name ON banking.accounts (customer_name);
CREATE INDEX idx_accounts_type ON banking.accounts (account_type);
CREATE INDEX idx_accounts_status ON banking.accounts (status);
CREATE INDEX idx_accounts_created_at ON banking.accounts (created_at);

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION banking.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to automatically update updated_at
CREATE TRIGGER update_accounts_updated_at BEFORE UPDATE
    ON banking.accounts FOR EACH ROW EXECUTE FUNCTION banking.update_updated_at_column();

-- Grant necessary permissions (adjust as needed for your security requirements)
-- These can be customized based on your specific user roles
GRANT USAGE ON SCHEMA banking TO PUBLIC;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA banking TO PUBLIC;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA banking TO PUBLIC;
