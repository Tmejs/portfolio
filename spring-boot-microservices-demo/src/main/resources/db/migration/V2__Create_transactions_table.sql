-- Set search path for this migration
SET search_path TO banking;

-- Create transactions table in banking schema
CREATE TABLE banking.transactions (
    id BIGSERIAL PRIMARY KEY,
    reference_number VARCHAR(50) NOT NULL UNIQUE,
    transaction_type VARCHAR(20) NOT NULL CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_IN', 'TRANSFER_OUT')),
    amount DECIMAL(19,2) NOT NULL CHECK (amount > 0),
    description VARCHAR(500),
    balance_after DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    account_id BIGINT NOT NULL REFERENCES banking.accounts(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_transactions_account_id ON banking.transactions (account_id);
CREATE INDEX idx_transactions_type ON banking.transactions (transaction_type);
CREATE INDEX idx_transactions_created_at ON banking.transactions (created_at);
CREATE UNIQUE INDEX idx_transactions_reference_number ON banking.transactions (reference_number);

-- Create composite index for account transactions ordered by date
CREATE INDEX idx_transactions_account_date ON banking.transactions (account_id, created_at DESC);

-- Grant necessary permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON banking.transactions TO PUBLIC;
GRANT USAGE, SELECT ON SEQUENCE banking.transactions_id_seq TO PUBLIC;
