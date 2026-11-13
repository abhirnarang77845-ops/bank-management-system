import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bson.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

// Handles every page of the website.
// It "implements HttpHandler", so Java calls handle() each time the browser asks for a page.
public class BankHandler implements HttpHandler {

    private Database db;

    // Who is logged in: session id (saved in the browser as a cookie) -> account number
    private HashMap<String, Integer> sessions = new HashMap<String, Integer>();

    public BankHandler(Database db) {
        this.db = db;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();

            // values typed in a form (POST) and values in the URL after "?" (GET)
            HashMap<String, String> form = parseParams(readBody(ex));
            HashMap<String, String> query = parseParams(ex.getRequestURI().getRawQuery());

            if (path.equals("/")) {
                loginPage(ex, query);
            } else if (path.equals("/login") && method.equals("POST")) {
                doLogin(ex, form);
            } else if (path.equals("/register") && method.equals("GET")) {
                registerPage(ex, query);
            } else if (path.equals("/register") && method.equals("POST")) {
                doRegister(ex, form);
            } else if (path.equals("/home")) {
                homePage(ex, query);
            } else if (path.equals("/deposit") && method.equals("POST")) {
                doDeposit(ex, form);
            } else if (path.equals("/withdraw") && method.equals("POST")) {
                doWithdraw(ex, form);
            } else if (path.equals("/transfer") && method.equals("POST")) {
                doTransfer(ex, form);
            } else if (path.equals("/history")) {
                historyPage(ex);
            } else if (path.equals("/logout")) {
                doLogout(ex);
            } else if (path.equals("/admin")) {
                adminPage(ex, form, method);
            } else {
                sendPage(ex, "Not found", "<h2>Page not found</h2><a href='/'>Go to login</a>");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendPage(ex, "Error", "<h2>Something went wrong</h2><p>" + escape(String.valueOf(e.getMessage()))
                    + "</p><a href='/'>Go back</a>");
        }
    }

    // ===================== LOGIN / REGISTER / LOGOUT =====================

    private void loginPage(HttpExchange ex, HashMap<String, String> query) throws IOException {
        if (getLoggedInAccount(ex) != null) {
            redirect(ex, "/home");
            return;
        }
        String html = "<h1>Bank Login</h1>"
                + messages(query)
                + "<form method='POST' action='/login'>"
                + "<label>Account Number</label><input name='accNo' required>"
                + "<label>PIN</label><input name='pin' type='password' required>"
                + "<button>Login</button>"
                + "</form>"
                + "<p><a href='/register'>Create new account</a> | <a href='/admin'>Admin</a></p>";
        sendPage(ex, "Login", html);
    }

    private void doLogin(HttpExchange ex, HashMap<String, String> form) throws IOException {
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

    private void registerPage(HttpExchange ex, HashMap<String, String> query) throws IOException {
        String html = "<h1>Create Account</h1>"
                + messages(query)
                + "<form method='POST' action='/register'>"
                + "<label>Account Type</label>"
                + "<select name='type'>"
                + "<option value='Savings'>Savings (min balance Rs 1000)</option>"
                + "<option value='Current'>Current (min opening Rs 5000)</option>"
                + "</select>"
                + "<label>Full Name</label><input name='name' required>"
                + "<label>4-digit PIN</label><input name='pin' type='password' required>"
                + "<label>Opening Deposit (Rs)</label><input name='amount' required>"
                + "<button>Create Account</button>"
                + "</form>"
                + "<p><a href='/'>Back to login</a></p>";
        sendPage(ex, "Create Account", html);
    }

    private void doRegister(HttpExchange ex, HashMap<String, String> form) throws IOException {
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

    private void doLogout(HttpExchange ex) throws IOException {
        String sessionId = getSessionId(ex);
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
        redirect(ex, "/?msg=" + encode("Logged out"));
    }

    // ===================== HOME (DASHBOARD) =====================

    private void homePage(HttpExchange ex, HashMap<String, String> query) throws IOException {
        Account acc = getAccountOrRedirect(ex);
        if (acc == null) {
            return;
        }

        String html = "<h1>Welcome, " + escape(acc.getName()) + "</h1>"
                + messages(query)
                + "<div class='card'>"
                + "<p>Account Number: <b>" + acc.getAccountNumber() + "</b></p>"
                + "<p>Account Type: <b>" + acc.getAccountType() + "</b></p>"
                + "<p class='balance'>Rs " + money(acc.getBalance()) + "</p>"
                + "</div>"

                + "<h3>Deposit</h3>"
                + "<form method='POST' action='/deposit'>"
                + "<input name='amount' placeholder='Amount' required><button>Deposit</button>"
                + "</form>"

                + "<h3>Withdraw</h3>"
                + "<form method='POST' action='/withdraw'>"
                + "<input name='amount' placeholder='Amount' required><button>Withdraw</button>"
                + "</form>"

                + "<h3>Transfer Money</h3>"
                + "<form method='POST' action='/transfer'>"
                + "<input name='toAccNo' placeholder='Receiver account number' required>"
                + "<input name='amount' placeholder='Amount' required><button>Transfer</button>"
                + "</form>"

                + "<p><a href='/history'>Transaction History</a> | <a href='/logout'>Logout</a></p>";
        sendPage(ex, "Home", html);
    }

    // ===================== DEPOSIT / WITHDRAW / TRANSFER =====================

    private void doDeposit(HttpExchange ex, HashMap<String, String> form) throws IOException {
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
        redirect(ex, "/home?msg=" + encode("Deposited Rs " + money(amount)));
    }

    private void doWithdraw(HttpExchange ex, HashMap<String, String> form) throws IOException {
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
            redirect(ex, "/home?msg=" + encode("Withdrawn Rs " + money(amount)));
        } catch (InsufficientFundsException e) {
            redirect(ex, "/home?err=" + encode(e.getMessage()));
        }
    }

    private void doTransfer(HttpExchange ex, HashMap<String, String> form) throws IOException {
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

            redirect(ex, "/home?msg=" + encode("Transferred Rs " + money(amount) + " to " + receiver.getName()));
        } catch (InsufficientFundsException e) {
            redirect(ex, "/home?err=" + encode(e.getMessage()));
        }
    }

    // ===================== HISTORY / ADMIN =====================

    private void historyPage(HttpExchange ex) throws IOException {
        Account acc = getAccountOrRedirect(ex);
        if (acc == null) {
            return;
        }

        String rows = "";
        for (Document t : db.getTransactions(acc.getAccountNumber())) {
            rows = rows + "<tr><td>" + escape(t.getString("date")) + "</td>"
                    + "<td>" + escape(t.getString("type")) + "</td>"
                    + "<td>" + money(t.getDouble("amount")) + "</td>"
                    + "<td>" + money(t.getDouble("balance")) + "</td></tr>";
        }

        String html = "<h1>Transaction History</h1>"
                + "<table><tr><th>Date</th><th>Type</th><th>Amount</th><th>Balance</th></tr>"
                + rows + "</table>"
                + "<p><a href='/home'>Back</a></p>";
        sendPage(ex, "History", html);
    }

    private void adminPage(HttpExchange ex, HashMap<String, String> form, String method) throws IOException {
        boolean correctPassword = method.equals("POST") && "admin123".equals(form.get("password"));

        if (!correctPassword) {
            String error = "";
            if (method.equals("POST")) {
                error = "<p class='err'>Wrong password</p>";
            }
            String html = "<h1>Admin Login</h1>" + error
                    + "<form method='POST' action='/admin'>"
                    + "<label>Admin Password</label><input name='password' type='password' required>"
                    + "<button>Login</button>"
                    + "</form>"
                    + "<p><a href='/'>Back</a></p>";
            sendPage(ex, "Admin", html);
            return;
        }

        String rows = "";
        ArrayList<Account> list = db.getAllAccounts();
        for (Account a : list) {
            rows = rows + "<tr><td>" + a.getAccountNumber() + "</td>"
                    + "<td>" + escape(a.getName()) + "</td>"
                    + "<td>" + a.getAccountType() + "</td>"
                    + "<td>" + money(a.getBalance()) + "</td></tr>";
        }

        String html = "<h1>All Accounts</h1>"
                + "<table><tr><th>Acc No</th><th>Name</th><th>Type</th><th>Balance</th></tr>"
                + rows + "</table>"
                + "<p>Total accounts: " + list.size() + "</p>"
                + "<p><a href='/'>Back</a></p>";
        sendPage(ex, "All Accounts", html);
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

    // Shows a green success message (msg) or red error message (err)
    private String messages(HashMap<String, String> query) {
        String html = "";
        if (query.get("msg") != null) {
            html = html + "<p class='msg'>" + escape(query.get("msg")) + "</p>";
        }
        if (query.get("err") != null) {
            html = html + "<p class='err'>" + escape(query.get("err")) + "</p>";
        }
        return html;
    }

    // Sends an HTML page to the browser
    private void sendPage(HttpExchange ex, String title, String body) throws IOException {
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<title>" + title + " - Bank</title>"
                + "<style>"
                + "body{font-family:Arial,sans-serif;background:#eef2f7;margin:0;padding:20px;}"
                + ".box{max-width:480px;margin:30px auto;background:white;padding:25px;border-radius:8px;"
                + "box-shadow:0 2px 8px rgba(0,0,0,0.1);}"
                + "h1{color:#1a4d8f;margin-top:0;}"
                + "label{display:block;margin-top:10px;font-weight:bold;}"
                + "input,select{width:100%;padding:8px;margin-top:4px;box-sizing:border-box;}"
                + "button{margin-top:10px;padding:9px 18px;background:#1a4d8f;color:white;border:none;"
                + "border-radius:4px;cursor:pointer;}"
                + ".card{background:#f4f8fd;padding:10px 15px;border-radius:6px;}"
                + ".balance{font-size:26px;font-weight:bold;color:#1a4d8f;}"
                + ".msg{background:#ddf5e1;color:#1d6b2f;padding:8px;border-radius:4px;}"
                + ".err{background:#fbe0e0;color:#a12020;padding:8px;border-radius:4px;}"
                + "table{width:100%;border-collapse:collapse;}"
                + "th,td{border:1px solid #ccc;padding:6px;text-align:left;font-size:14px;}"
                + "th{background:#1a4d8f;color:white;}"
                + "</style></head><body><div class='box'>" + body + "</div></body></html>";

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

    private String money(double amount) {
        return String.format("%.2f", amount);
    }

    private String encode(String text) throws IOException {
        return URLEncoder.encode(text, "UTF-8");
    }

    // Stops users from putting HTML code inside their name (security)
    private String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
