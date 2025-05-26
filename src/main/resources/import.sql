-- Note: These passwords are BCrypt encoded versions of:
-- admin: admin123
-- all others: password123

-- Insert Users
INSERT INTO users (username, password) VALUES
                                           ('admin', '$2a$10$YdXBPLPSTtVWsQ.hN0VZMeJ8o6u3yBTBfDY7GXkWQ1IEr7AHxH5bC'),
                                           ('john.doe', '$2a$10$5D1RMVj1ZG7m8.0nheD7Cu/FqLJmF9R2Zn6f6yZQV3xJWThLmZaFa'),
                                           ('jane.smith', '$2a$10$5D1RMVj1ZG7m8.0nheD7Cu/FqLJmF9R2Zn6f6yZQV3xJWThLmZaFa'),
                                           ('bob.wilson', '$2a$10$5D1RMVj1ZG7m8.0nheD7Cu/FqLJmF9R2Zn6f6yZQV3xJWThLmZaFa'),
                                           ('alice.brown', '$2a$10$5D1RMVj1ZG7m8.0nheD7Cu/FqLJmF9R2Zn6f6yZQV3xJWThLmZaFa'),
                                           ('charlie.davis', '$2a$10$5D1RMVj1ZG7m8.0nheD7Cu/FqLJmF9R2Zn6f6yZQV3xJWThLmZaFa'),
                                           ('david.miller', '$2a$10$5D1RMVj1ZG7m8.0nheD7Cu/FqLJmF9R2Zn6f6yZQV3xJWThLmZaFa'),
                                           ('emma.garcia', '$2a$10$5D1RMVj1ZG7m8.0nheD7Cu/FqLJmF9R2Zn6f6yZQV3xJWThLmZaFa');

-- Assign roles to users
INSERT INTO user_roles (user_id, role) VALUES
                                           (1, 'ADMIN'),
                                           (2, 'CUSTOMER'),
                                           (3, 'CUSTOMER'),
                                           (4, 'CUSTOMER'),
                                           (5, 'CUSTOMER'),
                                           (6, 'CUSTOMER'),
                                           (7, 'CUSTOMER'),
                                           (8, 'CUSTOMER');

-- Insert Customers (linked to users)
INSERT INTO customers (name, surname, credit_limit, used_credit_limit, user_id) VALUES
                                                                                    ('John', 'Doe', 10000.00, 0.00, 2),
                                                                                    ('Jane', 'Smith', 15000.00, 0.00, 3),
                                                                                    ('Bob', 'Wilson', 20000.00, 0.00, 4),
                                                                                    ('Alice', 'Brown', 12000.00, 0.00, 5),
                                                                                    ('Charlie', 'Davis', 8000.00, 0.00, 6),
                                                                                    ('David', 'Miller', 25000.00, 0.00, 7),
                                                                                    ('Emma', 'Garcia', 18000.00, 0.00, 8);
-- Sample Loans with different scenarios

-- Loan 1: Active loan for John Doe with 2 paid installments
INSERT INTO loans (customer_id, loan_amount, number_of_installment, interest_rate, create_date, is_paid) VALUES
    (1, 5000.00, 12, 0.15, DATEADD('MONTH', -2, CURRENT_TIMESTAMP), false);

-- Update John's used credit limit (5000 * 1.15 = 5750)
UPDATE customers SET used_credit_limit = 5750.00 WHERE id = 1;

-- Installments for Loan 1 (amount per installment: 5750 / 12 = 479.17)
INSERT INTO loan_installments (loan_id, amount, paid_amount, due_date, payment_date, is_paid) VALUES
-- Paid installments
(1, 479.17, 479.17, DATEADD('MONTH', -1, DATEADD('DAY', 1, DATEADD('DAY', -DAY(CURRENT_DATE), CURRENT_DATE))), DATEADD('DAY', -45, CURRENT_DATE), true),
(1, 479.17, 479.17, DATEADD('DAY', 1, DATEADD('DAY', -DAY(CURRENT_DATE), CURRENT_DATE)), DATEADD('DAY', -15, CURRENT_DATE), true),
-- Unpaid installments
(1, 479.17, 0.00, DATEADD('MONTH', 1, DATEADD('DAY', 1, DATEADD('DAY', -DAY(CURRENT_DATE), CURRENT_DATE))), null, false),
(1, 479.17, 0.00, DATEADD('MONTH', 2, DATEADD('DAY', 1, DATEADD('DAY', -DAY(CURRENT_DATE), CURRENT_DATE))), null, false),
(1, 479.17, 0.00, DATEADD('MONTH', 3, DATEADD('DAY', 1, DATEADD('DAY', -DAY(CURRENT_DATE), CURRENT_DATE))), null, false),
(1, 479.17, 0.00, DATEADD('MONTH', 4, DATEADD('DAY', 1, DATEADD('DAY', -DAY(CURRENT_DATE), CURRENT_DATE))), null, false),
(1, 479.17, 0.00, DATEADD('MONTH', 5, DATEADD('DAY', 1, DATEADD('DAY', -DAY(CURRENT_DATE), CURRENT_DATE))), null, false),
(1, 479.17, 0.00, DATEADD('MONTH', 6, DATEADD('DAY', 1, DATEADD('DAY', -DAY(CURRENT_DATE), CURRENT_DATE))), null, false),
(1, 479.17, 0.00, DATEADD('MONTH', 7, DATEADD('DAY', 1, DATEADD('DAY', -DAY(CURRENT_DATE), CURRENT_DATE))), null, false),
(1, 479.17, 0.00, DATEADD('MONTH', 8, DATEADD('DAY', 1, DATEADD('DAY', -DAY(CURRENT_DATE), CURRENT_DATE))), null, false),
(1, 479.17, 0.00, DATEADD('MONTH', 9, DATEADD('DAY', 1, DATEADD('DAY', -DAY(CURRENT_DATE), CURRENT_DATE))), null, false),
(1, 479.17, 0.00, DATEADD('MONTH', 10, DATEADD('DAY', 1, DATEADD('DAY', -DAY(CURRENT_DATE), CURRENT_DATE))), null, false);

-- Loan 2: Completed loan for Jane Smith
INSERT INTO loans (customer_id, loan_amount, number_of_installment, interest_rate, create_date, is_paid) VALUES
    (2, 3000.00, 6, 0.10, DATEADD('MONTH', -7, CURRENT_TIMESTAMP), true);

-- Loan 3: New loan for Bob Wilson (just created, no payments yet)
INSERT INTO loans (customer_id, loan_amount, number_of_installment, interest_rate, create_date, is_paid) VALUES
    (3, 8000.00, 24, 0.25, CURRENT_TIMESTAMP, false);

-- Update Bob's used credit limit (8000 * 1.25 = 10000)
UPDATE customers SET used_credit_limit = 10000.00 WHERE id = 3;

-- First 3 installments for Bob's loan (amount per installment: 10000 / 24 = 416.67)
INSERT INTO loan_installments (loan_id, amount, paid_amount, due_date, payment_date, is_paid) VALUES
                                                                                                  (3, 416.67, 0.00, DATEADD('MONTH', 1, DATEADD('DAY', 1, DATEADD('DAY', -DAY(CURRENT_DATE), CURRENT_DATE))), null, false),
                                                                                                  (3, 416.67, 0.00, DATEADD('MONTH', 2, DATEADD('DAY', 1, DATEADD('DAY', -DAY(CURRENT_DATE), CURRENT_DATE))), null, false),
                                                                                                  (3, 416.67, 0.00, DATEADD('MONTH', 3, DATEADD('DAY', 1, DATEADD('DAY', -DAY(CURRENT_DATE), CURRENT_DATE))), null, false);

-- Loan 4: Small loan for Alice Brown with late payment scenario
INSERT INTO loans (customer_id, loan_amount, number_of_installment, interest_rate, create_date, is_paid) VALUES
    (4, 2000.00, 6, 0.12, DATEADD('MONTH', -3, CURRENT_TIMESTAMP), false);

-- Update Alice's used credit limit (2000 * 1.12 = 2240)
UPDATE customers SET used_credit_limit = 2240.00 WHERE id = 4;

-- Installments for Alice (amount per installment: 2240 / 6 = 373.33)
INSERT INTO loan_installments (loan_id, amount, paid_amount, due_date, payment_date, is_paid) VALUES
-- Paid with late payment penalty
(4, 373.33, 380.00, DATEADD('MONTH', -2, DATEADD('DAY', 1, DATEADD('DAY', -DAY(CURRENT_DATE), CURRENT_DATE))), DATEADD('DAY', -50, CURRENT_DATE), true),
-- Paid on time
(4, 373.33, 373.33, DATEADD('MONTH', -1, DATEADD('DAY', 1, DATEADD('DAY', -DAY(CURRENT_DATE), CURRENT_DATE))), DATEADD('DAY', -30, CURRENT_DATE), true),
-- Overdue (current month)
(4, 373.33, 0.00, DATEADD('DAY', 1, DATEADD('DAY', -DAY(CURRENT_DATE), CURRENT_DATE)), null, false),
-- Future installments
(4, 373.33, 0.00, DATEADD('MONTH', 1, DATEADD('DAY', 1, DATEADD('DAY', -DAY(CURRENT_DATE), CURRENT_DATE))), null, false),
(4, 373.33, 0.00, DATEADD('MONTH', 2, DATEADD('DAY', 1, DATEADD('DAY', -DAY(CURRENT_DATE), CURRENT_DATE))), null, false),
(4, 373.33, 0.00, DATEADD('MONTH', 3, DATEADD('DAY', 1, DATEADD('DAY', -DAY(CURRENT_DATE), CURRENT_DATE))), null, false);