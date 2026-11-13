package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import database.Database;
import model.Account;
import model.CurrentAccount;
import model.InsufficientFundsException;
import model.SavingsAccount;
import view.Pages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.UUID;

// CONTROLLER: receives every request from the browser, decides what to do,
// uses the MODEL (Account classes) + DATABASE, then sends a VIEW (page) back.
// It "implements HttpHandler", so the web server calls handle() for each request.
public class BankController implements HttpHandler {

    private Database db;

    // Who is logged in: session id (saved in the browser as a cookie) -> account number
    private HashMap<String, Integer> sessions = new HashMap<String, Integer>();

    public BankController(Database db) {
        this.db = db;
    }

    // ===================== ROUTING =====================

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();

            // values typed in a form (POST) and values in the URL after "?" (GET)
            HashMap<String, String> form = parseParams(readBody(ex));
            HashMap<String, String> query = parseParams(ex.getRequestURI().getRawQuery());
            String msg = query.get("msg");
            String err = query.get("err");

            if (path.equals("/")) {
                showLogin(ex, msg, err);
            } else if (path.equals("/login") && method.equals("POST")) {
                login(ex, form);
            } else if (path.equals("/register") && method.equals("GET")) {
                send(ex, Pages.register(msg, err));
            } else if (path.equals("/register") && method.equals("POST")) {
                register(ex, form);
            } else if (path.equals("/home")) {
                showHome(ex, msg, err);
            } else if (path.equals("/deposit") && method.equals("POST")) {
                deposit(ex, form);
            } else if (path.equals("/withdraw") && method.equals("POST")) {
                withdraw(ex, form);
            } else if (path.equals("/transfer") && method.equals("POST")) {
                transfer(ex, form);
            } else if (path.equals("/history")) {
                showHistory(ex);
            } else if (path.equals("/logout")) {
                logout(ex);
            } else if (path.equals("/admin")) {
                admin(ex, form, method);
            } else {
                send(ex, Pages.notFound());
            }
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, Pages.error(String.valueOf(e.getMessage())));
        }
    }

    // ===================== LOGIN / REGISTER / LOGOUT =====================

    private void showLogin(HttpExchange ex, String msg, String err) throws IOException {
        if (getLoggedInAccount(ex) != null) {
            redirect(ex, "/home");
            return;
        }
        send(ex, Pages.login(msg, err));
    }

    private void login(HttpExchange ex, HashMap<String, String> form) throws IOException {
        int accNo = toInt(form.get("accNo"));
        int pin = toInt(form.get("pin"));

        Account acc = db.findAccount(accNo);
        if (acc == null || !acc.checkPin(pin)) {
            redirect(ex, "/?err=" + encode("Wrong account number or PIN"));
            return;
        }

        // Remember this browser as logged in
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, accNo);
        ex.getResponseHeaders().add("Set-Cookie", "session=" + sessionId + "; Path=/; HttpOnly");
        redirect(ex, "/home");
    }

    private void register(HttpExchange ex, HashMap<String, String> form) throws IOException {
        String type = form.get("type");
        String name = form.get("name");
        int pin = toInt(form.get("pin"));
        double amount = toDouble(form.get("amount"));

        if (name == null || name.trim().isEmpty()) {
            redirect(ex, "/register?err=" + encode("Please enter your name"));
            return;
        }
        if (pin < 1000 || pin > 9999) {
            redirect(ex, "/register?err=" + encode("PIN must be 4 digits"));
            return;
        }

        int accNo = db.getNextAccountNumber();
        Account acc;

        if ("Savings".equals(type)) {
            if (amount < SavingsAccount.MIN_BALANCE) {
                redirect(ex, "/register?err=" + encode("Savings account needs at least Rs 1000"));
                return;
            }
            acc = new SavingsAccount(accNo, name.trim(), pin, amount);
        } else {
            if (amount < 5000) {
                redirect(ex, "/register?err=" + encode("Current account needs at least Rs 5000"));
                return;
            }
            acc = new CurrentAccount(accNo, name.trim(), pin, amount);
        }

        db.addAccount(acc);
        db.addTransaction(accNo, "Account Opened", amount, amount);
        redirect(ex, "/?msg=" + encode("Account created! Your account number is " + accNo + ". Please login."));
    }

    private void logout(HttpExchange ex) throws IOException {
        String sessionId = getSessionId(ex);
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
        redirect(ex, "/?msg=" + encode("Logged out"));
    }

    // ===================== ACCOUNT PAGES =====================

    private void showHome(HttpExchange ex, String msg, String err) throws IOException {
        Account acc = getAccountOrRedirect(ex);
        if (acc == null) {
            return;
        }
        send(ex, Pages.home(acc, msg, err));
    }

    private void showHistory(HttpExchange ex) throws IOException {
        Account acc = getAccountOrRedirect(ex);
        if (acc == null) {
            return;
        }
        send(ex, Pages.history(db.getTransactions(acc.getAccountNumber())));
    }

    // ===================== DEPOSIT / WITHDRAW / TRANSFER =====================

    private void deposit(HttpExchange ex, HashMap<String, String> form) throws IOException {
        Account acc = getAccountOrRedirect(ex);
        if (acc == null) {
            return;
        }
        double amount = toDouble(form.get("amount"));
        if (amount <= 0) {
            redirect(ex, "/home?err=" + encode("Amount must be more than 0"));
            return;
        }

        acc.deposit(amount);
        db.updateBalance(acc);
        db.addTransaction(acc.getAccountNumber(), "Deposit", amount, acc.getBalance());
        redirect(ex, "/home?msg=" + encode("Deposited Rs " + Pages.money(amount)));
    }

    private void withdraw(HttpExchange ex, HashMap<String, String> form) throws IOException {
        Account acc = getAccountOrRedirect(ex);
        if (acc == null) {
            return;
        }
        double amount = toDouble(form.get("amount"));
        if (amount <= 0) {
            redirect(ex, "/home?err=" + encode("Amount must be more than 0"));
            return;
        }

        try {
            acc.withdraw(amount); // Polymorphism: Savings or Current version runs
            db.updateBalance(acc);
            db.addTransaction(acc.getAccountNumber(), "Withdraw", amount, acc.getBalance());
            redirect(ex, "/home?msg=" + encode("Withdrawn Rs " + Pages.money(amount)));
        } catch (InsufficientFundsException e) {
            redirect(ex, "/home?err=" + encode(e.getMessage()));
        }
    }

    private void transfer(HttpExchange ex, HashMap<String, String> form) throws IOException {
        Account acc = getAccountOrRedirect(ex);
        if (acc == null) {
            return;
        }
        int toAccNo = toInt(form.get("toAccNo"));
        double amount = toDouble(form.get("amount"));

        Account receiver = db.findAccount(toAccNo);
        if (receiver == null) {
            redirect(ex, "/home?err=" + encode("Receiver account not found"));
            return;
        }
        if (toAccNo == acc.getAccountNumber()) {
            redirect(ex, "/home?err=" + encode("Cannot transfer to your own account"));
            return;
        }
        if (amount <= 0) {
            redirect(ex, "/home?err=" + encode("Amount must be more than 0"));
            return;
        }

        try {
            acc.withdraw(amount);
            receiver.deposit(amount);

            db.updateBalance(acc);
            db.updateBalance(receiver);
            db.addTransaction(acc.getAccountNumber(), "Transfer to " + toAccNo, amount, acc.getBalance());
            db.addTransaction(toAccNo, "Transfer from " + acc.getAccountNumber(), amount, receiver.getBalance());

            redirect(ex, "/home?msg=" + encode("Transferred Rs " + Pages.money(amount) + " to " + receiver.getName()));
        } catch (InsufficientFundsException e) {
            redirect(ex, "/home?err=" + encode(e.getMessage()));
        }
    }

    // ===================== ADMIN =====================

    private void admin(HttpExchange ex, HashMap<String, String> form, String method) throws IOException {
        if (!method.equals("POST")) {
            send(ex, Pages.adminLogin(false));
        } else if ("admin123".equals(form.get("password"))) {
            send(ex, Pages.allAccounts(db.getAllAccounts()));
        } else {
            send(ex, Pages.adminLogin(true));
        }
    }

    // ===================== HELPER METHODS =====================

    // Returns the logged-in account, or sends the user to the login page
    private Account getAccountOrRedirect(HttpExchange ex) throws IOException {
        Integer accNo = getLoggedInAccount(ex);
        if (accNo == null) {
            redirect(ex, "/?err=" + encode("Please login first"));
            return null;
        }
        return db.findAccount(accNo);
    }

    private Integer getLoggedInAccount(HttpExchange ex) {
        String sessionId = getSessionId(ex);
        if (sessionId == null) {
            return null;
        }
        return sessions.get(sessionId);
    }

    // Reads "session=..." from the browser's cookie
    private String getSessionId(HttpExchange ex) {
        String cookie = ex.getRequestHeaders().getFirst("Cookie");
        if (cookie == null) {
            return null;
        }
        for (String part : cookie.split(";")) {
            part = part.trim();
            if (part.startsWith("session=")) {
                return part.substring("session=".length());
            }
        }
        return null;
    }

    // Sends an HTML page (made by the view) to the browser
    private void send(HttpExchange ex, String html) throws IOException {
        byte[] bytes = html.getBytes("UTF-8");
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(200, bytes.length);
        OutputStream out = ex.getResponseBody();
        out.write(bytes);
        out.close();
    }

    // Tells the browser to go to another page
    private void redirect(HttpExchange ex, String location) throws IOException {
        ex.getResponseHeaders().set("Location", location);
        ex.sendResponseHeaders(302, -1);
        ex.close();
    }

    // Reads the data the browser sent with a form (POST)
    private String readBody(HttpExchange ex) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(ex.getRequestBody(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    // Turns "name=Rahul&pin=1234" into a HashMap {name=Rahul, pin=1234}
    private HashMap<String, String> parseParams(String text) throws IOException {
        HashMap<String, String> map = new HashMap<String, String>();
        if (text == null || text.isEmpty()) {
            return map;
        }
        for (String pair : text.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], "UTF-8");
            String value = "";
            if (parts.length > 1) {
                value = URLDecoder.decode(parts[1], "UTF-8");
            }
            map.put(key, value);
        }
        return map;
    }

    // Converts text to a number, returns -1 if it is not a valid number
    private int toInt(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return -1;
        }
    }

    private double toDouble(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (Exception e) {
            return -1;
        }
    }

    private String encode(String text) throws IOException {
        return URLEncoder.encode(text, "UTF-8");
    }
}
