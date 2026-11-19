package com.smartbank;

import java.util.Scanner;

/** Console UI. Run with "api" argument to start the REST server instead. */
public class Main {
    public static void main(String[] args) throws Exception {
        DB.init();
        BankService bank = new BankService();

        if (args.length > 0 && args[0].equals("api")) {
            int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
            ApiServer.start(bank, port);
            return;
        }

        Scanner in = new Scanner(System.in);
        int userId = -1;
        while (userId < 0) {
            System.out.print("1) Register  2) Login : ");
            String choice = in.nextLine();
            System.out.print("Username: "); String u = in.nextLine();
            System.out.print("Password: "); String p = in.nextLine();
            try {
                if (choice.equals("1")) bank.register(u, p);
                userId = bank.login(u, p);
                if (userId < 0) System.out.println("Invalid credentials.");
            } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
        }

        String acc = null;
        while (true) {
            System.out.print("\n1 Create account | 2 Balance | 3 Deposit | 4 Withdraw | 5 History | 0 Exit : ");
            String ch = in.nextLine();
            try {
                switch (ch) {
                    case "1" -> { acc = bank.createAccount(userId); System.out.println("Account: " + acc); }
                    case "2" -> System.out.println("Balance: " + bank.balance(ask(in, acc)));
                    case "3" -> { String a = ask(in, acc); System.out.print("Amount: ");
                                  bank.deposit(a, Double.parseDouble(in.nextLine())); System.out.println("Done."); }
                    case "4" -> { String a = ask(in, acc); System.out.print("Amount: ");
                                  bank.withdraw(a, Double.parseDouble(in.nextLine())); System.out.println("Done."); }
                    case "5" -> bank.history(ask(in, acc)).forEach(t ->
                                  System.out.printf("%s  %-8s %.2f%n", t.time(), t.type(), t.amount()));
                    case "0" -> { return; }
                    default -> System.out.println("Invalid option");
                }
            } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
        }
    }

    private static String ask(Scanner in, String last) {
        System.out.print("Account no" + (last != null ? " [" + last + "]" : "") + ": ");
        String s = in.nextLine().trim();
        return s.isEmpty() ? last : s;
    }
}
