/*
 * Manager.java
 *
 * Version:
 *     $Id$
 *
 * Revisions:
 *     $Log$
 */


/**
 * Executes the thread operations
 *
 * @author Ria Lulla
 */


import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Manager {
    private static final String SQLConnection = "jdbc:mysql://localhost:3306/assignment_1_tdm";
    private static final String SQLUser = "root";
    private static final String SQLPassword = "root";
    public static int totalOperations = 0;

    /**
     * The main function.Test the performance of your application with a number of concurrent threads
     * ranging from 1-10. (That is, you will test once with one thread, once with two threads, and so
     * on.) For a period of five minutes, all threads should repeatedly execute one of the operations
     * you have implemented in a loop
     *
     * @param args command line args (ignored)
     */
    public static void main(String[] args) throws SQLException {

        Operations op = new Operations(SQLConnection, SQLUser, SQLPassword);
        HashMap<Integer,Integer> hm = new HashMap<>();
        //test the performance of your application with a number of concurrent thread ranging from 1-10.
        for (int i = 1; i <= 10; i++) {
            int noOfOperations = 0;
            System.out.println("Thread " + i);
            op.initializeDB();
            long startTime = System.currentTimeMillis();
            long elapsedTime = 0L;
            //runs for a period of 5 minutes
            while (elapsedTime < 5 * 60 * 1000) {
                noOfOperations++;
                ExecutorService executorService = Executors.newFixedThreadPool(i);
                Runnable operations = new Operations(SQLConnection, SQLUser, SQLPassword);
                int k = 0;
                while (k++ < i) {
                    executorService.execute(operations);
                }
                elapsedTime = (new Date()).getTime() - startTime;
            }
            //clean DB after each test
            op.clearDB();
            hm.put(i,noOfOperations);
            hm.entrySet().forEach(entry -> {
                System.out.println("Hashmap Entries :"+ entry.getKey() + " " + entry.getValue());
            });
            for (Integer val : hm.values()) {
                totalOperations += val;
            }
            System.out.println("Total number of operations : "+totalOperations);
            //System.out.println("Operations completed");

        }

    }


}
