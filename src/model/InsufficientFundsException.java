package model;

// Custom (user-defined) exception, thrown when there is not enough balance
public class InsufficientFundsException extends Exception {

    public InsufficientFundsException(String message) {
        super(message);
    }
}
