// Inheritance: CurrentAccount IS-A Account
public class CurrentAccount extends Account {

    public static final double OVERDRAFT_LIMIT = 10000;

    public CurrentAccount(int accountNumber, String name, int pin, double balance) {
        super(accountNumber, name, pin, balance); // calls the Account constructor
    }

    // Polymorphism (method overriding): current account allows overdraft,
    // so the balance can go below 0, down to -10000
    @Override
    public void withdraw(double amount) throws InsufficientFundsException {
        if (balance - amount < -OVERDRAFT_LIMIT) {
            throw new InsufficientFundsException(
                    "Overdraft limit of Rs " + OVERDRAFT_LIMIT + " exceeded");
        }
        balance = balance - amount;
    }

    @Override
    public String getAccountType() {
        return "Current";
    }
}
