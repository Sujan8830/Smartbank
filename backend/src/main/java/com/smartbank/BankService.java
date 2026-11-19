package com.smartbank;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/** Core banking operations. All money movements run inside a DB transaction. */
public class BankService {
    private static final Logger LOG = Logger.getLogger(BankService.class.getName());

    private static String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    public void register(String user, String pass) throws SQLException {
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("INSERT INTO users(username,password_hash) VALUES(?,?)")) {
            ps.setString(1, user);
            ps.setString(2, hash(pass));
            ps.executeUpdate();
            LOG.info("Registered user " + user);
        }
    }

    /** @return user id, or -1 if credentials are wrong */
    public int login(String user, String pass) throws SQLException {
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("SELECT id FROM users WHERE username=? AND password_hash=?")) {
            ps.setString(1, user);
            ps.setString(2, hash(pass));
            ResultSet r = ps.executeQuery();
            boolean ok = r.next();
            LOG.info("Login " + (ok ? "success" : "failed") + " for " + user);
            return ok ? r.getInt(1) : -1;
        }
    }

    public String createAccount(int userId) throws SQLException {
        String accNo = "SB" + (100000 + new Random().nextInt(900000));
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("INSERT INTO accounts(user_id,acc_no) VALUES(?,?)")) {
            ps.setInt(1, userId);
            ps.setString(2, accNo);
            ps.executeUpdate();
        }
        LOG.info("Created account " + accNo);
        return accNo;
    }

    public double balance(String accNo) throws SQLException {
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("SELECT balance FROM accounts WHERE acc_no=?")) {
            ps.setString(1, accNo);
            ResultSet r = ps.executeQuery();
            if (!r.next()) throw new IllegalArgumentException("Account not found");
            return r.getDouble(1);
        }
    }

    public void deposit(String accNo, double amt)  throws SQLException { apply(accNo, amt, "DEPOSIT"); }
    public void withdraw(String accNo, double amt) throws SQLException { apply(accNo, amt, "WITHDRAW"); }

    private void apply(String accNo, double amt, String type) throws SQLException {
        if (amt <= 0) throw new IllegalArgumentException("Amount must be positive");
        try (Connection c = DB.get()) {
            c.setAutoCommit(false);
            try {
                int id; double bal;
                try (PreparedStatement ps = c.prepareStatement("SELECT id,balance FROM accounts WHERE acc_no=?")) {
                    ps.setString(1, accNo);
                    ResultSet r = ps.executeQuery();
                    if (!r.next()) throw new IllegalArgumentException("Account not found");
                    id = r.getInt(1);
                    bal = r.getDouble(2);
                }
                double newBal = type.equals("DEPOSIT") ? bal + amt : bal - amt;
                if (newBal < 0) throw new IllegalArgumentException("Insufficient funds");

                try (PreparedStatement ps = c.prepareStatement("UPDATE accounts SET balance=? WHERE id=?")) {
                    ps.setDouble(1, newBal);
                    ps.setInt(2, id);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO transactions(account_id,type,amount) VALUES(?,?,?)")) {
                    ps.setInt(1, id);
                    ps.setString(2, type);
                    ps.setDouble(3, amt);
                    ps.executeUpdate();
                }
                c.commit();
                LOG.info(type + " " + amt + " on " + accNo + " -> balance " + newBal);
            } catch (Exception e) {
                c.rollback();
                LOG.warning("Rolled back " + type + " on " + accNo + ": " + e.getMessage());
                throw e;
            }
        }
    }

    public List<Txn> history(String accNo) throws SQLException {
        List<Txn> list = new ArrayList<>();
        String sql = "SELECT t.type,t.amount,t.created_at FROM transactions t "
                   + "JOIN accounts a ON a.id=t.account_id WHERE a.acc_no=? ORDER BY t.id DESC";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, accNo);
            ResultSet r = ps.executeQuery();
            while (r.next()) list.add(new Txn(r.getString(1), r.getDouble(2), r.getString(3)));
        }
        return list;
    }
}
