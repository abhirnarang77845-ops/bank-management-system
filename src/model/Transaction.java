package model;

// MODEL: one entry in the transaction history (deposit, withdraw, transfer...)
public class Transaction {

    private String date;
    private String type;
    private double amount;
    private double balanceAfter;

    public Transaction(String date, String type, double amount, double balanceAfter) {
        this.date = date;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }

    public String getDate() {
        return date;
    }

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }
}
