package model;

// MODEL: holds account data and the banking rules.
// Abstract class: we never create a plain "Account" object,
// only SavingsAccount or CurrentAccount objects.
public abstract class Account {

    // Encapsulation: data is private, other classes use getters/methods
    private int accountNumber;
    private String name;
    private int pin;
    protected double balance; // protected so child classes can change it

    // Constructor
    public Account(int accountNumber, String name, int pin, double balance) {
        this.accountNumber = accountNumber;
        this.name = name;
        this.pin = pin;
        this.balance = balance;
    }

    // Getters
    public int getAccountNumber() {
        return accountNumber;
    }

    public String getName() {
        return name;
    }

    public int getPin() {
        return pin;
    }

    public double getBalance() {
        return balance;
    }

    public boolean checkPin(int enteredPin) {
        return pin == enteredPin;
    }

    // Same for every account type
    public void deposit(double amount) {
        balance = balance + amount;
    }

    // Abstract methods: every child class MUST write its own version
    public abstract void withdraw(double amount) throws InsufficientFundsException;

    public abstract String getAccountType();
}
