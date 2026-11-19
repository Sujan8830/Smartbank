-- PL/SQL (Oracle) version of a fund transfer. SQLite has no PL/SQL,
-- so this is the stored-procedure flavour of the same logic for Oracle.
CREATE OR REPLACE PROCEDURE transfer_funds (
    p_from_acc IN VARCHAR2,
    p_to_acc   IN VARCHAR2,
    p_amount   IN NUMBER
) AS
    v_balance accounts.balance%TYPE;
BEGIN
    IF p_amount <= 0 THEN
        RAISE_APPLICATION_ERROR(-20001, 'Amount must be positive');
    END IF;

    SELECT balance INTO v_balance FROM accounts
     WHERE acc_no = p_from_acc FOR UPDATE;

    IF v_balance < p_amount THEN
        RAISE_APPLICATION_ERROR(-20002, 'Insufficient funds');
    END IF;

    UPDATE accounts SET balance = balance - p_amount WHERE acc_no = p_from_acc;
    UPDATE accounts SET balance = balance + p_amount WHERE acc_no = p_to_acc;

    INSERT INTO transactions (account_id, type, amount)
    SELECT id, 'WITHDRAW', p_amount FROM accounts WHERE acc_no = p_from_acc;
    INSERT INTO transactions (account_id, type, amount)
    SELECT id, 'DEPOSIT', p_amount FROM accounts WHERE acc_no = p_to_acc;

    COMMIT;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        ROLLBACK;
        RAISE_APPLICATION_ERROR(-20003, 'Account not found');
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END transfer_funds;
/
