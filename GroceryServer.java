import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class GroceryServer {
    static final int PORT = Integer.parseInt(
            System.getenv().getOrDefault("PORT", "8080"));
    static Connection con;

    public static void main(String[] args) throws Exception {
        con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/warehouse_inventory_db",
                "root", "");

        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);

        server.createContext("/", exchange -> serveFile(exchange, "index.html", "text/html"));
        server.createContext("/style.css", exchange -> serveFile(exchange, "style.css", "text/css"));
        server.createContext("/app.js", exchange -> serveFile(exchange, "app.js", "application/javascript"));
        server.createContext("/api/register", GroceryServer::register);
        server.createContext("/api/login", GroceryServer::login);
        server.createContext("/api/products", GroceryServer::products);
        server.createContext("/api/add-product", GroceryServer::addProduct);
        server.createContext("/api/stack", GroceryServer::stack);
        server.createContext("/api/push", GroceryServer::push);
        server.createContext("/api/pop", GroceryServer::pop);

        server.start();
        System.out.println("Warehouse Inventory System running at http://localhost:" + PORT);
    }

    static void serveFile(HttpExchange e, String file, String type) throws IOException {
        File f = new File(file);
        if (!f.exists()) {
            send(e, 404, "File not found");
            return;
        }
        e.getResponseHeaders().set("Content-Type", type + "; charset=UTF-8");
        byte[] data = java.nio.file.Files.readAllBytes(f.toPath());
        e.sendResponseHeaders(200, data.length);
        e.getResponseBody().write(data);
        e.close();
    }

    static void register(HttpExchange e) throws IOException {
        Map<String, String> p = params(e);
        String u = p.getOrDefault("username", "").trim();
        String pw = p.getOrDefault("password", "");

        if (u.isEmpty() || pw.isEmpty()) {
            sendJson(e, "{\"success\":false,\"message\":\"Please fill in all fields.\"}");
            return;
        }
        if (u.length() < 3 || pw.length() < 4) {
            sendJson(e,
                    "{\"success\":false,\"message\":\"Username must be at least 3 characters and password at least 4 characters.\"}");
            return;
        }

        try (PreparedStatement s = con.prepareStatement(
                "INSERT INTO users(username,password) VALUES(?,?)")) {
            s.setString(1, u);
            s.setString(2, pw);
            s.executeUpdate();
            sendJson(e, "{\"success\":true,\"message\":\"Registration successful.\"}");
        } catch (SQLIntegrityConstraintViolationException ex) {
            sendJson(e, "{\"success\":false,\"message\":\"Username already exists.\"}");
        } catch (Exception ex) {
            sendJson(e, "{\"success\":false,\"message\":\"Database error.\"}");
        }
    }

    static void login(HttpExchange e) throws IOException {
        Map<String, String> p = params(e);
        String u = p.getOrDefault("username", "");
        String pw = p.getOrDefault("password", "");
        boolean ok = false;
        try (PreparedStatement s = con.prepareStatement(
                "SELECT id FROM users WHERE username=? AND password=?")) {
            s.setString(1, u);
            s.setString(2, pw);
            ResultSet r = s.executeQuery();
            ok = r.next();
        } catch (Exception ex) {
        }
        sendJson(e, "{\"success\":" + ok + "}");
    }

    static void products(HttpExchange e) throws IOException {
        StringBuilder b = new StringBuilder("[");
        try (Statement s = con.createStatement();
                ResultSet r = s.executeQuery(
                        "SELECT p.id,p.name,COALESCE(SUM(st.quantity),0) stock " +
                                "FROM products p LEFT JOIN stock st ON p.id=st.product_id " +
                                "GROUP BY p.id,p.name ORDER BY p.name")) {
            boolean first = true;
            while (r.next()) {
                if (!first)
                    b.append(",");
                first = false;
                b.append("{\"id\":").append(r.getInt("id"))
                        .append(",\"name\":\"").append(json(r.getString("name")))
                        .append("\",\"stock\":").append(r.getInt("stock")).append("}");
            }
        } catch (Exception ex) {
            b.append("{\"error\":\"database error\"}");
        }
        b.append("]");
        sendJson(e, b.toString());
    }

    static void addProduct(HttpExchange e) throws IOException {
        Map<String, String> p = params(e);
        String name = p.getOrDefault("name", "").trim();

        if (name.isEmpty()) {
            sendJson(e, "{\"success\":false,\"message\":\"Enter a product name.\"}");
            return;
        }

        try (PreparedStatement check = con.prepareStatement(
                "SELECT id FROM products WHERE LOWER(name)=LOWER(?)")) {
            check.setString(1, name);
            if (check.executeQuery().next()) {
                sendJson(e, "{\"success\":false,\"message\":\"Product already exists.\"}");
                return;
            }
        } catch (Exception ex) {
            sendJson(e, "{\"success\":false,\"message\":\"Database error.\"}");
            return;
        }

        try (PreparedStatement s = con.prepareStatement(
                "INSERT INTO products(name) VALUES(?)")) {
            s.setString(1, name);
            s.executeUpdate();
            sendJson(e, "{\"success\":true,\"message\":\"Product added.\"}");
        } catch (Exception ex) {
            sendJson(e, "{\"success\":false,\"message\":\"Could not add product.\"}");
        }
    }

    static void stack(HttpExchange e) throws IOException {
        Map<String, String> p = params(e);
        int id = Integer.parseInt(p.getOrDefault("product_id", "0"));
        StringBuilder b = new StringBuilder("[");
        try (PreparedStatement s = con.prepareStatement(
                "SELECT id,box_code,quantity,created_at FROM stock WHERE product_id=? AND quantity>0 ORDER BY id DESC")) {
            s.setInt(1, id);
            ResultSet r = s.executeQuery();
            boolean first = true;
            while (r.next()) {
                if (!first)
                    b.append(",");
                first = false;
                b.append("{\"id\":").append(r.getInt("id"))
                        .append(",\"box\":\"").append(json(r.getString("box_code")))
                        .append("\",\"quantity\":").append(r.getInt("quantity"))
                        .append(",\"created\":\"").append(json(String.valueOf(r.getTimestamp("created_at"))))
                        .append("\"}");
            }
        } catch (Exception ex) {
        }
        b.append("]");
        sendJson(e, b.toString());
    }

    static void push(HttpExchange e) throws IOException {
        Map<String, String> p = params(e);
        int product = Integer.parseInt(p.getOrDefault("product_id", "0"));
        int qty = Integer.parseInt(p.getOrDefault("quantity", "0"));
        String box = p.getOrDefault("box", "BOX-" + System.currentTimeMillis());

        if (qty <= 0) {
            sendJson(e, "{\"success\":false,\"message\":\"Quantity must be greater than 0.\"}");
            return;
        }

        try (PreparedStatement s = con.prepareStatement(
                "INSERT INTO stock(product_id,box_code,quantity) VALUES(?,?,?)")) {
            s.setInt(1, product);
            s.setString(2, box);
            s.setInt(3, qty);
            s.executeUpdate();
            sendJson(e, "{\"success\":true}");
        } catch (Exception ex) {
            sendJson(e, "{\"success\":false,\"message\":\"Could not add stock.\"}");
        }
    }

    static void pop(HttpExchange e) throws IOException {
        Map<String, String> p = params(e);
        int product = Integer.parseInt(p.getOrDefault("product_id", "0"));
        int qty = Integer.parseInt(p.getOrDefault("quantity", "1"));

        if (qty <= 0) {
            sendJson(e, "{\"success\":false,\"message\":\"Quantity must be greater than 0.\"}");
            return;
        }

        try (PreparedStatement s = con.prepareStatement(
                "SELECT id,quantity FROM stock WHERE product_id=? AND quantity>0 ORDER BY id DESC LIMIT 1")) {
            s.setInt(1, product);
            ResultSet r = s.executeQuery();
            if (!r.next()) {
                sendJson(e, "{\"success\":false,\"message\":\"Stack is empty.\"}");
                return;
            }
            int id = r.getInt("id"), current = r.getInt("quantity");
            if (qty >= current) {
                try (PreparedStatement d = con.prepareStatement("UPDATE stock SET quantity=0 WHERE id=?")) {
                    d.setInt(1, id);
                    d.executeUpdate();
                }
            } else {
                try (PreparedStatement u = con.prepareStatement("UPDATE stock SET quantity=quantity-? WHERE id=?")) {
                    u.setInt(1, qty);
                    u.setInt(2, id);
                    u.executeUpdate();
                }
            }
            sendJson(e, "{\"success\":true,\"message\":\"Top stock removed using LIFO.\"}");
        } catch (Exception ex) {
            sendJson(e, "{\"success\":false,\"message\":\"Could not remove stock.\"}");
        }
    }

    static Map<String, String> params(HttpExchange e) throws IOException {
        String q = e.getRequestURI().getQuery();
        Map<String, String> m = new HashMap<>();
        if (q == null)
            return m;
        for (String x : q.split("&")) {
            String[] a = x.split("=", 2);
            if (a.length == 2)
                m.put(URLDecoder.decode(a[0], "UTF-8"), URLDecoder.decode(a[1], "UTF-8"));
        }
        return m;
    }

    static String json(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static void sendJson(HttpExchange e, String s) throws IOException {
        e.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        send(e, 200, s);
    }

    static void send(HttpExchange e, int code, String s) throws IOException {
        byte[] d = s.getBytes(StandardCharsets.UTF_8);
        e.sendResponseHeaders(code, d.length);
        e.getResponseBody().write(d);
        e.close();
    }
}