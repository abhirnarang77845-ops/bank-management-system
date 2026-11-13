import com.sun.net.httpserver.HttpServer;
import controller.BankController;
import database.Database;

import java.net.InetSocketAddress;

// Starts the bank website on http://localhost:8080
public class Main {

    public static void main(String[] args) throws Exception {
        Database db = new Database();

        // Java's built-in web server, listening on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8080), 0);

        // Every request (every page) is sent to the controller
        server.createContext("/", new BankController(db));
        server.start();

        System.out.println("Bank website is running!");
        System.out.println("Open this in your browser: http://localhost:8080");
        System.out.println("Press Ctrl+C here to stop it.");
    }
}
