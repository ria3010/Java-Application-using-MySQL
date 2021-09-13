/*
 * Operations.java
 *
 * Version:
 *     $Id$
 *
 * Revisions:
 *     $Log$
 */


/**
 * Application Operations and Evaluation
 *
 * @author Ria Lulla
 */


import org.apache.ibatis.jdbc.ScriptRunner;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Operations implements OperationsInterface, Runnable {

    private static final String CREATE_ACCOUNT_SQL = "INSERT INTO Users (username, password, first_name,last_name) VALUES (?,?,?,?)";
    private static final String USER_EXISTS_SQL = "SELECT * FROM Users WHERE username = ? AND password = ?";
    private static final String ADD_PRODUCT_SQL = "INSERT INTO Products(product_name,description,price,numItems) VALUES(?,?,?,?)";
    private static final String GET_STOCK_ITEM_SQL = "SELECT NUMITEMS FROM PRODUCTS WHERE PRODUCT_ID = ?";
    private static final String UPDATE_STOCK_SQL = "UPDATE PRODUCTS SET NUMITEMS = ? WHERE PRODUCT_ID = ?";
    private static final String POST_REVIEW_SQL = "INSERT INTO Reviews(description,rating,review_date,username_fk,product_id)VALUES(?,?,?,?,?)";
    private static final String AVERAGE_SQL = "SELECT CAST(AVG(rating) AS DECIMAL(10,2)) AS RATING FROM Reviews where username_fk = ?";
    private static final String GETINFO_SQL = "SELECT Reviews.username_fk, Reviews.description, Reviews.rating, Reviews.product_id,Products.product_name FROM Reviews JOIN Products ON Products.product_id = Reviews.product_id where Reviews.product_id = ?";
    private static final String PRODUCT_AVALABILITY_SQL = "SELECT PRODUCT_ID,PRODUCT_NAME,NUMITEMS FROM PRODUCTS WHERE PRODUCT_NAME = ?";
    private static final String ORDER_ITEMS_SQL = "INSERT INTO ORDERS(quantity,order_date,product_id,purchasing_user) VALUES(?,?,?,?)";
    private static final String CLEANDB_SQL = "DELETE FROM REVIEWS;DELETE FROM PRODUCTS;DELETE FROM ORDERS;DELETE FROM USERS;";
    String SQLConnection;
    String SQLUser;
    String SQLPassword;

    /*
     * Constructor - to initialize the connection */
    public Operations(String connection, String SQLuser, String SQLpassword) {
        this.SQLConnection = connection;
        this.SQLUser = SQLuser;
        this.SQLPassword = SQLpassword;
    }

    /**
     * Establish a SQL Connection
     *
     * @return SQL connection
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(SQLConnection, SQLUser, SQLPassword);
    }

    /**
     * Establishes a new account for the user. This should fail if the username already exists.
     *
     * @param username   username
     * @param password   password
     * @param first_name first name of the user
     * @param last_name  last name of the user
     */
    public void createAccount(String username, String password, String first_name, String last_name) throws SQLException {
        Connection connection = getConnection();
        try {
            PreparedStatement ps_createAccount = connection.prepareStatement(CREATE_ACCOUNT_SQL);
            connection.setAutoCommit(false);
            ps_createAccount.setString(1, username);
            ps_createAccount.setString(2, password);
            ps_createAccount.setString(3, first_name);
            ps_createAccount.setString(4, last_name);
            ps_createAccount.execute();
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLIntegrityConstraintViolationException constraintViolationException) {
            //System.out.println("User with same username already exists. Try another username");
        } catch (Exception exception) {
            //exception.printStackTrace();
        } finally {
            connection.close();
        }

    }

    /**
     * First the username and password to be checked to ensure the order is authorized. (Note
     * that this is not a secure way to implement such a system, but it will suffice for our
     * purposes.) After authorization, you should check that the items are available. If any of
     * the items are not available in the desired quantity, the order is not submitted. Orders
     * should not be submitted if any item is unavailable in the requested quantity. Otherwise, a
     * record for the order is created and the stock levels are reduced accordingly. There is no
     * need to keep a record of an order which fails.
     *
     * @param username   username
     * @param password   password
     * @param products   list of products
     * @param quantities list of quantity
     */
    public void submitOrder(String username, String password, List<String> products, List<Integer> quantities) throws SQLException {
        Connection connection = getConnection();
        connection.setAutoCommit(false);
        try {
            HashMap<String, Integer> hmap = new HashMap<>();
            String productName = null;
            int quantity = 0;
            int product_id = 0;
            Iterator<String> i1 = products.iterator();
            Iterator<Integer> i2 = quantities.iterator();
            while (i1.hasNext() && i2.hasNext()) {
                hmap.put(i1.next(), i2.next());
            }

            PreparedStatement ps_checkUser = connection.prepareStatement(USER_EXISTS_SQL);
            ps_checkUser.setString(1, username);
            ps_checkUser.setString(2, password);
            ResultSet result = ps_checkUser.executeQuery();
            if (result.next()) {
                for (String product : products) {
                    PreparedStatement ps_productAvailability = connection.prepareStatement(PRODUCT_AVALABILITY_SQL);
                    ps_productAvailability.setString(1, product);
                    ResultSet resultSet = ps_productAvailability.executeQuery();
                    if (resultSet.isBeforeFirst()) {
                        while (resultSet.next()) {
                            productName = resultSet.getString("product_name");
                            quantity = resultSet.getInt("numItems");
                            product_id = resultSet.getInt("product_id");
                        }
                        int orderedQuantity = hmap.get(productName);
                        if (quantity - orderedQuantity >= 0) {

                            int finalQuantity = quantity - orderedQuantity;
                            PreparedStatement ps_orderItems = connection.prepareStatement(ORDER_ITEMS_SQL);
                            ps_orderItems.setInt(1, orderedQuantity);
                            Date orderDate = new Date(new java.util.Date().getTime());
                            ps_orderItems.setDate(2, orderDate);
                            ps_orderItems.setInt(3, product_id);
                            ps_orderItems.setString(4, username);
                            ps_orderItems.execute();
                            connection.commit();
                            PreparedStatement ps_reduceProducts = connection.prepareStatement(UPDATE_STOCK_SQL);
                            ps_reduceProducts.setInt(1, finalQuantity);
                            ps_reduceProducts.setInt(2, product_id);
                            ps_reduceProducts.execute();
                            connection.commit();
                        } else {
                            //System.out.println("No stock available, Order failed");
                        }
                    } else {
                        //System.out.println("Order failed");
                    }


                }
            } else {
                //System.out.println("Authentication failed");
            }
            connection.setAutoCommit(true);
        } catch (Exception e) {
            //e.printStackTrace();
        } finally {
            connection.close();

        }

    }

    /**
     * A new product is added to the database with the given name and description and the
     * given initial stock value. This operation should provide an ID for the product which can
     * be used in future operations.
     *
     * @param name         Product name
     * @param description  product description
     * @param price        price of the product
     * @param initialStock initial stock quantity
     */

    public void addProduct(String name, String description, float price, int initialStock) throws SQLException {
        Connection connection = getConnection();
        try {
            PreparedStatement ps_addProduct = connection.prepareStatement(ADD_PRODUCT_SQL);
            connection.setAutoCommit(false);
            ps_addProduct.setString(1, name);
            ps_addProduct.setString(2, description);
            ps_addProduct.setFloat(3, price);
            ps_addProduct.setInt(4, initialStock);
            ps_addProduct.execute();
            connection.commit();
            connection.setAutoCommit(true);

        } finally {
            connection.close();
        }


    }

    /**
     * Adds new inventory associated with the product, adding to the current stock level.
     *
     * @param productId      Product id
     * @param itemCountToAdd items to add
     */
    public void updateStockLevel(int productId, int itemCountToAdd) throws SQLException {
        Connection connection = getConnection();
        try {
            int numberOfItems = 0;
            PreparedStatement ps_getStockLevel = connection.prepareStatement(GET_STOCK_ITEM_SQL);
            connection.setAutoCommit(false);
            ps_getStockLevel.setInt(1, productId);
            ResultSet resultSet = ps_getStockLevel.executeQuery();
            if (resultSet.isBeforeFirst()) {
                while (resultSet.next()) {
                    numberOfItems = resultSet.getInt("numItems") + itemCountToAdd;
                }
                ps_getStockLevel = connection.prepareStatement(UPDATE_STOCK_SQL);
                ps_getStockLevel.setInt(1, numberOfItems);
                ps_getStockLevel.setInt(2, productId);
                ps_getStockLevel.execute();
                connection.commit();
                connection.setAutoCommit(true);
            }
        } finally {
            connection.close();
        }
    }

    /**
     * First authorizes the user (as above) and then submits a review. Each user should also
     * only be able to post a single review for a given product.
     *
     * @param username   username of user
     * @param password   password of user
     * @param productId  productId
     * @param rating     ratings of the product
     * @param reviewText review text of the product
     * @param reviewDate review date of the product
     */

    public void postReview(String username, String password, int productId, String rating, String reviewText, Date reviewDate) throws SQLException {
        Connection connection = getConnection();
        try {
            connection.setAutoCommit(false);
            PreparedStatement ps_checkUser = connection.prepareStatement(USER_EXISTS_SQL);
            ps_checkUser.setString(1, username);
            ps_checkUser.setString(2, password);
            ResultSet result = ps_checkUser.executeQuery();
            if (result.next()) {
                PreparedStatement ps_postReview = connection.prepareStatement(POST_REVIEW_SQL);
                ps_postReview.setString(1, reviewText);
                ps_postReview.setString(2, rating);
                ps_postReview.setDate(3, reviewDate);
                ps_postReview.setString(4, username);
                ps_postReview.setInt(5, productId);
                ps_postReview.execute();
                connection.commit();
            } else {
                //System.out.println("No username/password found");
            }

            connection.setAutoCommit(true);
            connection.close();
        } catch (SQLIntegrityConstraintViolationException constraintViolationException) {
            //System.out.println("User with same username has already posted a review.");
        } finally {
            connection.close();
        }

    }

    /**
     * Get the average rating value for a given user by adding the ratings for all products and
     * dividing by the total number of reviews the user has provided.
     *
     * @param username username of user
     * @return average ratings of the given user
     */

    public float getAverageUserRatings(String username) throws SQLException {
        Connection connection = getConnection();
        float avgRating = 0;
        try {
            PreparedStatement ps_getAverage = connection.prepareStatement(AVERAGE_SQL);
            ps_getAverage.setString(1, username);
            ResultSet resultSet = ps_getAverage.executeQuery();
            while (resultSet.next()) {
                avgRating = resultSet.getInt("rating");
            }
        } catch (SQLException e) {
            //e.printStackTrace();
        }

        //System.out.println(avgRating);
        finally {
            connection.close();

        }
        return avgRating;
    }

    /**
     * Return the product information and all the reviews for the given product including the
     * username of the reviewing user, the rating, and the text of the review
     *
     * @param product_id product_id of the user
     * @return result set of the product and review
     */

    public ResultSet getProductAndReviews(int product_id) throws SQLException {
        Connection connection = getConnection();
        ResultSet resultSet;
        try {
            connection.setAutoCommit(false);
            PreparedStatement ps_getProductsAndReviews = connection.prepareStatement(GETINFO_SQL);
            ps_getProductsAndReviews.setInt(1, product_id);
            resultSet = ps_getProductsAndReviews.executeQuery();
            while (resultSet.next()) {
                String username = resultSet.getString(1);
                String description = resultSet.getString(2);
                String rating = resultSet.getString(3);
                int productId = resultSet.getInt(4);
                String productName = resultSet.getString(5);
                //System.out.println(username+" "+ description+" "+ rating+" "+product_id+" "+productName+" ");
            }
            connection.setAutoCommit(true);
        } finally {
            connection.close();
        }
        return resultSet;


    }

    /**
     * Generating random create account data
     *
     * @param count
     */

    public void generateRandomCreateAccount(int count) throws SQLException {
        for (int i = 0; i < count; i++) {
            String username = "user" + i;
            String password = "password" + i;
            String first_name = "firstName" + i;
            String last_name = "lastName" + i;
            createAccount(username, password, first_name, last_name);
        }
    }

    /**
     * Generating random add product data
     *
     * @param count
     */
    public void generateRandomAddProduct(int count) throws SQLException {
        //Connection connection = getConnection();
        for (int i = 0; i < count; i++) {
            String product = "product" + i;
            String description = "description" + i;
            float price = nextFloatBetween(10.0f, 100.0f);
            int initialStock = nextIntBetween(1, 100);
            addProduct(product, description, price, initialStock);
        }
    }

    /**
     * Generating post review random data
     *
     * @param count
     */
    public void generatePostReview(int count) {
        try {
            for (int i = 0; i < count; i++) {
                int userRandomInt = nextIntBetween(1, 1000);
                String username = "user" + userRandomInt;
                String password = "password" + userRandomInt;
                int productRandomId = nextIntBetween(1, 10000);
                int productId = productRandomId;
                String rating = "" + nextIntBetween(1, 5);
                String reviewText = "reviewText" + nextIntBetween(1, 20000);
                Date reviewdate = Date.valueOf("2020-01-01");
                postReview(username, password, productId, rating, reviewText, reviewdate);
            }

        } catch (Exception e) {
            //e.printStackTrace();
        }

    }

    /**
     * Generating random submit order data - SubmitOrder × 10,000 with a random user and 10 random products each time along with
     * a random date in the past year (considering 10 product orders * 1000 users)
     *
     * @param count
     */

    public void generateRandomSubmitOrder(int count) throws SQLException {
        for (int i = 0; i < count; i++) {
            int userRandomInt = nextIntBetween(1, 1000);
            String username = "user" + userRandomInt;
            String password = "password" + userRandomInt;
            List<String> products = new ArrayList<>();
            List<Integer> quantities = new ArrayList<>();
            for (int j = 0; j < 10; j++) {
                int productRandomInt = nextIntBetween(1, 10000);
                int quantityRandomInt = nextIntBetween(1, 100);
                String product = "product" + productRandomInt;
                products.add(product);
                quantities.add(quantityRandomInt);
            }
            submitOrder(username, password, products, quantities);
        }

    }

    /**
     * Generating random float number
     *
     * @param min
     * @param max
     */
    public static float nextFloatBetween(float min, float max) {
        return (ThreadLocalRandom.current().nextFloat() * (max - min)) + min;
    }

    /**
     * Generating random int number
     *
     * @param min
     * @param max
     */
    public static int nextIntBetween(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * initializing the db - make use of script runner that runs the dump file stored in the given location each time so as to save time
     */
    public void initializeDB() throws SQLException {
        System.out.println("Initialization started...");
        Connection connection = getConnection();
        try {
            ScriptRunner sr = new ScriptRunner(connection);
            Reader reader = new BufferedReader(new FileReader("C:\\Users\\Ria\\Documents\\CSCI-729 Topics in Data Management\\Assignment1-rkl2498\\Dump20210911.sql"));
            sr.runScript(reader);
            System.out.println("Initialization ended");
        } catch (FileNotFoundException e) {
            //e.printStackTrace();
        } finally {
            connection.close();
        }

    }

    /**
     * Cleans the db after every test
     */
    public void clearDB() throws SQLException {
        Connection connection = getConnection();
        try {
            connection.setAutoCommit(false);
            PreparedStatement ps_clearReviews = connection.prepareStatement(CLEANDB_SQL);
            ps_clearReviews.execute();
            connection.commit();
            connection.setAutoCommit(true);
        } catch (Exception e) {
        } finally {
            connection.close();

        }

    }

    /**
     * Thread run() method - calls probability Distribution of each task method
     */
    @Override
    public void run() {
        try {

            //System.out.println(Thread.currentThread().getName() + " started");
            probabilityDistributionMethods();
            //System.out.println(Thread.currentThread().getName() + " ended");

        } catch (SQLException e) {
            //e.printStackTrace();
        }

    }

    /**
     * probability distribution of each method
     * ● 3%, execute the CreateAccount operation with a random user
     * ● 2%, execute the AddProduct operation with a random product
     * ● 10%, execute the UpdateStockLevel operation for a random product
     * ● 65%, execute the GetProductAndReviews operation for a random product
     * ● 5%, execute the GetAverageUserRating operation for a random user and product
     * ● 10%, execute the SubmitOrder operation with a random user and 10 random products
     * ● 5%, execute PostReview operation for a random user and product
     */
    public void probabilityDistributionMethods() throws SQLException {
        float randomNum = nextFloatBetween(0, 1);
        if (randomNum <= 0.03) {
            int randNumGenerator = nextIntBetween(500, 2500);
            String username = "user" + randNumGenerator;
            String password = "password" + randNumGenerator;
            String first_name = "firstName" + randNumGenerator;
            String last_name = "lastName" + randNumGenerator;
            createAccount(username, password, first_name, last_name);
        } else if (randomNum <= 0.03 + 0.02) {
            int randomProductGenerator = nextIntBetween(5000, 15000);
            String product = "product" + randomProductGenerator;
            String description = "description" + randomProductGenerator;
            float price = nextFloatBetween(10.0f, 100.0f);
            int initialStock = nextIntBetween(1, 100);
            addProduct(product, description, price, initialStock);
        } else if (randomNum <= 0.03 + 0.02 + 0.1) {
            int productId = nextIntBetween(1, 10000);
            int itemCountToAdd = nextIntBetween(1, 100);
            updateStockLevel(productId, itemCountToAdd);
        } else if (randomNum <= 0.03 + 0.02 + 0.1 + 0.65) {
            int product_id = nextIntBetween(1, 10000);
            getProductAndReviews(product_id);
        } else if (randomNum <= 0.03 + 0.02 + 0.1 + 0.65 + 0.05) {
            int randUsername = nextIntBetween(1, 1000);
            String username = "username" + randUsername;
            float avgRating = getAverageUserRatings(username);
        } else if (randomNum <= 0.03 + 0.02 + 0.1 + 0.65 + 0.05 + 0.1) {
            generateRandomSubmitOrder(100);
        } else if (randomNum <= 0.03 + 0.02 + 0.1 + 0.65 + 0.05 + 0.1 + 0.05) {
            generatePostReview(100);
        }
    }
}
