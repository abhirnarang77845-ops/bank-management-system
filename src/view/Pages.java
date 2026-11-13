package view;

import model.Account;
import model.Transaction;

import java.util.ArrayList;

// VIEW: only builds the HTML for each page. No logic, no database here.
// The controller decides WHICH page to show and gives it the data.
public class Pages {

    // ===================== PAGES =====================

    public static String login(String msg, String err) {
        String body = "<h1>Bank Login</h1>"
                + messages(msg, err)
                + "<form method='POST' action='/login'>"
                + "<label>Account Number</label><input name='accNo' required>"
                + "<label>PIN</label><input name='pin' type='password' required>"
                + "<button>Login</button>"
                + "</form>"
                + "<p><a href='/register'>Create new account</a> | <a href='/admin'>Admin</a></p>";
        return layout("Login", body);
    }

    public static String register(String msg, String err) {
        String body = "<h1>Create Account</h1>"
                + messages(msg, err)
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
        return layout("Create Account", body);
    }

    public static String home(Account acc, String msg, String err) {
        String body = "<h1>Welcome, " + escape(acc.getName()) + "</h1>"
                + messages(msg, err)
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
        return layout("Home", body);
    }

    public static String history(ArrayList<Transaction> list) {
        String rows = "";
        for (Transaction t : list) {
            rows = rows + "<tr><td>" + escape(t.getDate()) + "</td>"
                    + "<td>" + escape(t.getType()) + "</td>"
                    + "<td>" + money(t.getAmount()) + "</td>"
                    + "<td>" + money(t.getBalanceAfter()) + "</td></tr>";
        }
        String body = "<h1>Transaction History</h1>"
                + "<table><tr><th>Date</th><th>Type</th><th>Amount</th><th>Balance</th></tr>"
                + rows + "</table>"
                + "<p><a href='/home'>Back</a></p>";
        return layout("History", body);
    }

    public static String adminLogin(boolean wrongPassword) {
        String error = "";
        if (wrongPassword) {
            error = "<p class='err'>Wrong password</p>";
        }
        String body = "<h1>Admin Login</h1>" + error
                + "<form method='POST' action='/admin'>"
                + "<label>Admin Password</label><input name='password' type='password' required>"
                + "<button>Login</button>"
                + "</form>"
                + "<p><a href='/'>Back</a></p>";
        return layout("Admin", body);
    }

    public static String allAccounts(ArrayList<Account> list) {
        String rows = "";
        for (Account a : list) {
            rows = rows + "<tr><td>" + a.getAccountNumber() + "</td>"
                    + "<td>" + escape(a.getName()) + "</td>"
                    + "<td>" + a.getAccountType() + "</td>"
                    + "<td>" + money(a.getBalance()) + "</td></tr>";
        }
        String body = "<h1>All Accounts</h1>"
                + "<table><tr><th>Acc No</th><th>Name</th><th>Type</th><th>Balance</th></tr>"
                + rows + "</table>"
                + "<p>Total accounts: " + list.size() + "</p>"
                + "<p><a href='/'>Back</a></p>";
        return layout("All Accounts", body);
    }

    public static String notFound() {
        return layout("Not found", "<h2>Page not found</h2><a href='/'>Go to login</a>");
    }

    public static String error(String message) {
        return layout("Error", "<h2>Something went wrong</h2><p>" + escape(message)
                + "</p><a href='/'>Go back</a>");
    }

    // ===================== HELPERS =====================

    // Common design (HTML head + CSS) used by every page
    private static String layout(String title, String body) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
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
    }

    // Green success message or red error message
    private static String messages(String msg, String err) {
        String html = "";
        if (msg != null) {
            html = html + "<p class='msg'>" + escape(msg) + "</p>";
        }
        if (err != null) {
            html = html + "<p class='err'>" + escape(err) + "</p>";
        }
        return html;
    }

    public static String money(double amount) {
        return String.format("%.2f", amount);
    }

    // Stops users from putting HTML code inside their name (security)
    public static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
