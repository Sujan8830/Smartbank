package com.smartbank;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/** Minimal REST API using the JDK's built-in HttpServer (no extra libraries). */
public class ApiServer {
    public static void start(BankService bank, int port) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/balance", ex -> handle(ex, q ->
            "{\"balance\":" + bank.balance(q.get("acc")) + "}"));
        server.createContext("/api/deposit", ex -> handle(ex, q -> {
            bank.deposit(q.get("acc"), Double.parseDouble(q.get("amount")));
            return "{\"status\":\"ok\"}";
        }));
        server.createContext("/api/withdraw", ex -> handle(ex, q -> {
            bank.withdraw(q.get("acc"), Double.parseDouble(q.get("amount")));
            return "{\"status\":\"ok\"}";
        }));
        server.createContext("/api/history", ex -> handle(ex, q ->
            bank.history(q.get("acc")).stream()
                .map(t -> String.format("{\"type\":\"%s\",\"amount\":%s,\"time\":\"%s\"}",
                                        t.type(), t.amount(), t.time()))
                .collect(Collectors.joining(",", "[", "]"))));

        server.createContext("/api/register", ex -> handle(ex, q -> {
            bank.register(q.get("user"), q.get("pass"));
            int id = bank.login(q.get("user"), q.get("pass"));
            return "{\"account\":\"" + bank.createAccount(id) + "\"}";
        }));

        server.start();
        System.out.println("API running on http://localhost:" + port);
    }

    interface Action { String run(Map<String, String> params) throws Exception; }

    private static void handle(HttpExchange ex, Action action) throws java.io.IOException {
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*"); // lets Angular (port 4200) call it
        int code = 200;
        String body;
        try {
            body = action.run(parse(ex.getRequestURI().getQuery()));
        } catch (IllegalArgumentException e) {
            code = 400; body = "{\"error\":\"" + e.getMessage() + "\"}";
        } catch (Exception e) {
            code = 500; body = "{\"error\":\"Server error\"}";
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    private static Map<String, String> parse(String query) {
        Map<String, String> m = new HashMap<>();
        if (query == null) return m;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) m.put(kv[0], kv[1]);
        }
        return m;
    }
}
