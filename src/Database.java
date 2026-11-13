import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

// All MongoDB code is in this one class
public class Database {

    private MongoClient client;
    private MongoCollection<Document> accounts;
    private MongoCollection<Document> transactions;

    // Connect to MongoDB running on this computer
    public Database() {
        client = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase db = client.getDatabase("bankdb");
        accounts = db.getCollection("accounts");
        transactions = db.getCollection("transactions");
    }

    // New account number = biggest account number + 1 (first one is 1001)
    public int getNextAccountNumber() {
        Document last = accounts.find().sort(new Document("accNo", -1)).first();
        if (last == null) {
            return 1001;
        }
        return last.getInteger("accNo") + 1;
    }

    // Save a new account
    public void addAccount(Account acc) {
        Document doc = new Document("accNo", acc.getAccountNumber())
                .append("name", acc.getName())
                .append("pin", acc.getPin())
                .append("type", acc.getAccountType())
                .append("balance", acc.getBalance());
        accounts.insertOne(doc);
    }

    // Find an account by number, returns null if not found
    public Account findAccount(int accNo) {
        Document doc = accounts.find(new Document("accNo", accNo)).first();
        if (doc == null) {
            return null;
        }
        return toAccount(doc);
    }

    // Get every account (for admin page)
    public ArrayList<Account> getAllAccounts() {
        ArrayList<Account> list = new ArrayList<Account>();
        for (Document doc : accounts.find()) {
            list.add(toAccount(doc));
        }
        return list;
    }

    // Save the new balance after deposit / withdraw / transfer
    public void updateBalance(Account acc) {
        Document filter = new Document("accNo", acc.getAccountNumber());
        Document update = new Document("$set", new Document("balance", acc.getBalance()));
        accounts.updateOne(filter, update);
    }

    // Save one transaction (deposit, withdraw, transfer...)
    public void addTransaction(int accNo, String type, double amount, double balanceAfter) {
        String date = new SimpleDateFormat("dd-MM-yyyy HH:mm").format(new Date());
        Document doc = new Document("accNo", accNo)
                .append("type", type)
                .append("amount", amount)
                .append("balance", balanceAfter)
                .append("date", date);
        transactions.insertOne(doc);
    }

    // Get all transactions of one account
    public ArrayList<Document> getTransactions(int accNo) {
        ArrayList<Document> list = new ArrayList<Document>();
        for (Document doc : transactions.find(new Document("accNo", accNo))) {
            list.add(doc);
        }
        return list;
    }

    // Convert a MongoDB document into the correct Java object
    private Account toAccount(Document doc) {
        int accNo = doc.getInteger("accNo");
        String name = doc.getString("name");
        int pin = doc.getInteger("pin");
        double balance = doc.getDouble("balance");
        String type = doc.getString("type");

        if (type.equals("Savings")) {
            return new SavingsAccount(accNo, name, pin, balance);
        } else {
            return new CurrentAccount(accNo, name, pin, balance);
        }
    }

    public void close() {
        client.close();
    }
}
