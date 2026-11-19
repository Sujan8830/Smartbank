package com.smartbank;

/** Immutable transaction record (OOP: simple value object). */
public record Txn(String type, double amount, String time) { }
