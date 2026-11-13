package model;

// Inheritance: SavingsAccount IS-A Account
public class SavingsAccount extends Account {

    public static final double MIN_BALANCE = 1000;

    public SavingsAccount(int accountNumber, String name, int pin, double balance) {
        super(accountNumber, name, pin, balance); // calls the Account constructor
    }

    // Polymorphism (method overriding): savings must keep Rs 1000 minimum
    @Override
    public void withdraw(double amount) throws InsufficientFundsException {
        if (balance - amount < MIN_BALANCE) {
            throw new InsufficientFundsException(
                    "Savings account must keep minimum balance of Rs " + MIN_BALANCE);
        }
        balance = balance - amount;
    }

    @Override
    public String getAccountType() {
        return "Savings";
    }
}
