import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface OperationsInterface {
    void createAccount(String username, String password, String first_name, String last_name) throws SQLException;

    void submitOrder(String username, String password, List<String> products, List<Integer> productQuantity) throws SQLException;

    void addProduct(String name, String description, float price, int initialStock) throws SQLException;

    void updateStockLevel(int productId, int itemCountToAdd) throws SQLException;

    void postReview(String username, String password, int productId, String rating, String reviewText, Date reviewDate) throws SQLException;

    float getAverageUserRatings(String username) throws SQLException;

    ResultSet getProductAndReviews(int product_id) throws SQLException;


}
