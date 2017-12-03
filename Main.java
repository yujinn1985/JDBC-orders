package ua.i.licit;

import java.math.BigDecimal;
import java.sql.*;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {
    private static final String DB_CONNECTION = "jdbc:mysql://localhost:3306/orderDB";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);
             Statement st = conn.createStatement(); Scanner sc = new Scanner(System.in)) {
            createTables(st);
            insertProducts(conn, sc);
            System.out.println();
            insertCustomers(conn, sc);
            System.out.println();
            placeOrderDB(conn, st, sc);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void placeOrderDB(Connection conn, Statement st, Scanner sc) {
        while (true) {
            System.out.println("1: add an order");
            System.out.println("2: delete an order");
            System.out.println("3: view orders");
            System.out.println("4: view clients");
            System.out.println("5: view products");
            System.out.print("-> ");
            sc = new Scanner(System.in);
            String choice = sc.nextLine();
            switch (choice) {
                case "1":
                    addOrder(conn, sc);
                    System.out.println();
                    break;
                case "2":
                    deleteOrder(conn, sc);
                    System.out.println();
                    break;
                case "3":
                    viewOrders(st);
                    System.out.println();
                    break;
                case "4":
                    viewCustomers(st);
                    System.out.println();
                    break;
                case "5":
                    viewProducts(st);
                    System.out.println();
                    break;
                default:
                    return;
            }
        }
    }

    private static void createTables(Statement st) throws SQLException {
        st.execute("DROP TABLE IF EXISTS orders");
        st.execute("DROP TABLE IF EXISTS customers");
        st.execute("DROP TABLE IF EXISTS products");
        st.execute("CREATE TABLE customers (" +
                "cust_num INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                "first_name VARCHAR (30) NOT NULL, " +
                "surname VARCHAR (30) NOT NULL, " +
                "address VARCHAR (30) DEFAULT NULL, " +
                "phone VARCHAR (15) NOT NULL" +
                ")");
        st.execute("CREATE TABLE products (" +
                "prod_num INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                "description VARCHAR(128) NOT NULL, " +
                "price DECIMAL(5,2) NOT NULL" +
                ")");
        st.execute("CREATE TABLE orders (" +
                "ord_num INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                "amount DOUBLE NOT NULL, " +
                "date DATE DEFAULT NULL, " +
                "product INT DEFAULT NULL, " +
                "customer INT DEFAULT NULL, " +
                "FOREIGN KEY (product) REFERENCES products (prod_num) ON DELETE CASCADE, " +
                "FOREIGN KEY (customer) REFERENCES customers (cust_num) ON DELETE CASCADE" +
                ")");
    }

    private static void insertProducts(Connection conn, Scanner sc) {
        System.out.print("Enter the number of products to be created:");
        int x = sc.nextInt();
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO products " +
                    "(description, price) VALUES(?,?)")) {
                createProducts(ps, sc, x);
                conn.commit();
                System.out.println("The products have been successfully added.");
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                conn.rollback();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createProducts(PreparedStatement ps, Scanner sc, int x) {
        BigDecimal price;
        for (int i = 0; i < x; i++) {
            System.out.println("Creating " + (i + 1) + "...");
            for (; ; ) {
                sc = new Scanner(System.in);
                try {
                    System.out.print("Enter a price of the new product: ");
                    String text = sc.nextLine();
                    price = new BigDecimal(text);
                    ps.setString(1, "text " + (i + 1));
                    ps.setBigDecimal(2, price);
                    ps.executeUpdate();
                    break;
                } catch (NoSuchElementException | SQLException | NumberFormatException e) {
                    System.out.println(e);
                    System.out.println("Try again...");
                }
            }
        }
    }

    private static void insertCustomers(Connection conn, Scanner sc) {
        System.out.print("Enter the number of customers to be created:");
        int x = sc.nextInt();
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO customers " +
                    "(first_name, surname, address, phone) VALUES(?,?,?,?)")) {
                createCustomers(ps, sc, x);
                conn.commit();
                System.out.println("The customers have been successfully added.");
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                conn.rollback();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createCustomers(PreparedStatement ps, Scanner sc, int x) {
        String text;
        for (int i = 0; i < x; i++) {
            for (; ; ) {
                System.out.println("Creating " + (i + 1) + "...");
                sc = new Scanner(System.in);
                try {
                    System.out.print("Enter a first name of the new client: ");
                    text = sc.nextLine();
                    ps.setString(1, text);
                    System.out.print("Enter a surname of the new client: ");
                    text = sc.nextLine();
                    ps.setString(2, text);
                    System.out.print("Enter an address of the new client: ");
                    text = sc.nextLine();
                    ps.setString(3, text);
                    System.out.print("Enter a phone of the new client: ");
                    text = sc.nextLine();
                    ps.setString(4, text);
                    ps.executeUpdate();
                    break;
                } catch (NoSuchElementException | SQLException e) {
                    System.out.println(e);
                    System.out.println("Try again...");
                }
            }
        }
    }

    private static void addOrder(Connection conn, Scanner sc) {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO orders " +
                "(amount, date, product, customer) VALUES(?,?,(SELECT prod_num FROM products WHERE prod_num=?)," +
                "(SELECT cust_num FROM customers WHERE cust_num=?))")) {
            placeOrder(ps, sc);
            System.out.println("The order has been successfully added.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void placeOrder(PreparedStatement ps, Scanner sc) {
        double amount;
        int id;
        for (; ; ) {
            sc = new Scanner(System.in);
            try {
                System.out.print("Enter an amount of the new order: ");
                amount = sc.nextDouble();
                ps.setDouble(1, amount);
                ps.setDate(2, new Date(System.currentTimeMillis()));
                System.out.print("Enter an id of the product in this order: ");
                id = sc.nextInt();
                ps.setInt(3, id);
                System.out.print("Enter an id of the customer in this order: ");
                id = sc.nextInt();
                ps.setInt(4, id);
                ps.executeUpdate();
                break;
            } catch (NoSuchElementException | SQLException e) {
                System.out.println(e);
                System.out.println("Try again...");
            }
        }
    }

    private static void deleteOrder(Connection conn, Scanner sc) {
        System.out.print("Enter order id: ");
        int id = sc.nextInt();
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM orders WHERE ord_num = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void viewCustomers(Statement st) {
        try (ResultSet rs = st.executeQuery("SELECT * FROM customers")) {
            viewResultSet(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void viewProducts(Statement st) {
        try (ResultSet rs = st.executeQuery("SELECT * FROM products")) {
            viewResultSet(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void viewOrders(Statement st) {
        try (ResultSet rs = st.executeQuery("SELECT * FROM orders")) {
            viewResultSet(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void viewResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        for (int i = 1; i <= md.getColumnCount(); i++)
            System.out.print(md.getColumnName(i) + "\t\t");
        System.out.println();
        while (rs.next()) {
            for (int i = 1; i <= md.getColumnCount(); i++) {
                System.out.print(rs.getString(i) + "\t\t\t");
            }
            System.out.println();
        }
    }
}
