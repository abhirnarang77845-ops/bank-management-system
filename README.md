# Bank Management System (Website)

A simple Java OOP project that runs as a small website and stores data in **MongoDB**.

## How to run

1. Make sure MongoDB is running (it runs automatically as a Windows service).
2. Double-click `run.bat`, or open `src/Main.java` in VS Code and click ▶ Run.
3. Open **http://localhost:8080** in your browser.
4. To stop the website, press `Ctrl+C` in the terminal window.

Admin password: `admin123`

### Demo accounts (already in the database)

| Acc No | Name | Type | PIN | Balance |
|--------|------|------|-----|---------|
| 1001 | Rahul Sharma | Savings | 1234 | Rs 3000 |
| 1002 | Priya Singh | Current | 4321 | Rs -9000 (using overdraft) |

## Pages

| Page | What it does |
|------|--------------|
| `/` | Login page (account number + PIN) |
| `/register` | Create a new Savings or Current account |
| `/home` | Dashboard: balance, deposit, withdraw, transfer |
| `/history` | Transaction history table |
| `/admin` | Admin password, then a list of all accounts |
| `/logout` | Logs out |

## Files

| File | What it does |
|------|--------------|
| `Account.java` | Abstract parent class: account number, name, PIN, balance |
| `SavingsAccount.java` | Child class: must keep minimum Rs 1000 |
| `CurrentAccount.java` | Child class: can go up to Rs 10000 below zero (overdraft) |
| `InsufficientFundsException.java` | Our own exception for "not enough balance" |
| `Database.java` | All MongoDB code: save, find, update accounts and transactions |
| `BankHandler.java` | Makes the web pages and handles the forms (login, deposit...) |
| `Main.java` | Starts the web server on port 8080 |
| `lib/` | MongoDB Java driver jar files (library needed to connect to MongoDB) |

## How the website works (simple explanation)

1. `Main` starts Java's **built-in web server** (`HttpServer`, part of the JDK, so no framework).
2. When you open a page, the browser sends a **request**. The server passes it to `BankHandler.handle()`.
3. `handle()` checks the **path** (like `/login` or `/deposit`) and calls the matching method.
4. That method uses the OOP classes and `Database`, then sends back an **HTML page**.
5. Forms use **POST** to send data (amount, PIN...). After saving, the user is **redirected**
   back to `/home` with a message like "Deposited Rs 500".
6. **Login session:** after a correct PIN, we create a random id (UUID), store
   `id → account number` in a `HashMap`, and give the browser a **cookie** with that id.
   On each page we read the cookie to know who is logged in. Logout removes it.

## MongoDB

Database name: `bankdb`

**accounts** collection, one document per account:
```
{ accNo: 1001, name: "Rahul Sharma", pin: 1234, type: "Savings", balance: 3000.0 }
```

**transactions** collection, one document per deposit / withdraw / transfer:
```
{ accNo: 1001, type: "Deposit", amount: 1000.0, balance: 6000.0, date: "13-11-2026 10:29" }
```

MongoDB methods used: `insertOne()`, `find()`, `updateOne()` with `$set`.

## OOP concepts (viva answers)

**1. Class and Object**
`Account` is a class. `new SavingsAccount(1001, "Rahul", 1234, 5000)` creates an object.

**2. Encapsulation**
Fields like `name`, `pin` and `accountNumber` in `Account` are `private`.
Other classes can only read them using getters like `getName()`.

**3. Inheritance**
`SavingsAccount` and `CurrentAccount` use `extends Account`. They get
`deposit()`, `getBalance()`, `checkPin()` from the parent without writing them again.
`super(...)` calls the parent constructor.

**4. Polymorphism (method overriding)**
Both child classes have their own `withdraw()` method:
- Savings: cannot go below Rs 1000
- Current: can go down to -10000

In `BankHandler`, we just write `acc.withdraw(amount)`. Java decides at runtime which
version to run, based on whether `acc` is a Savings or Current object.

**5. Abstraction**
`Account` is an `abstract` class with abstract methods `withdraw()` and
`getAccountType()`. We cannot create an `Account` object directly, only its child classes.

**6. Interface**
`BankHandler implements HttpHandler`. `HttpHandler` is an interface from Java that has one
method, `handle()`. We write that method, and the web server calls it for every request.

**7. Exception Handling**
`InsufficientFundsException` is our own exception (it `extends Exception`).
`withdraw()` throws it, and `BankHandler` catches it with `try-catch` to show a red
error message on the page instead of crashing.

## Common viva questions

- **Why abstract class?** Every account has a different withdraw rule, so `Account`
  only says "every account must have withdraw()" and the child classes decide how.
- **Why MongoDB?** It stores data permanently. Without a database, all accounts
  would be lost when the program closes.
- **What is a document / collection?** A document is one record (like a row in MySQL).
  A collection is a group of documents (like a table).
- **Which web framework did you use?** None. Java's built-in `com.sun.net.httpserver`
  package, with HTML written as Java strings.
- **What is GET and POST?** GET opens a page. POST sends form data to the server.
- **What is a cookie?** A small piece of text the browser stores and sends back on every
  request. We use it to remember who is logged in.
- **What does `escape()` do?** It stops someone from putting HTML code in their name
  (for example `<script>`), which could break or attack the page.
- **What are the jar files in lib?** The MongoDB Java driver: ready-made code that lets
  Java talk to MongoDB. The slf4j jars just stop the driver from printing log warnings.
- **`.java` vs `.class`?** `.java` is our source code. `javac` compiles it into `.class`
  (bytecode), which the JVM runs.
