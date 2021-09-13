# Java-Application-using-MySQL
This application uses a relational database. Makes direct use of the operations the application implements when measuring the performance of the application.

Part 1 :


Data Modeling :
● Users: A user account consists of a username and password as well as the user’s first
and last name. Usernames should be able to uniquely identify a user (two users may
have the same first and last name).
● Products: A product should have a unique product identifier as well as a name, a
description, the price, and the current number of the product in stock.
● Reviews: Users may post reviews of products which include a text component as well
as a rating. You should also store the date of the review. Users can only write a single
review for each product.
● Orders: Each user may have one or more orders of products. Each order will have a
unique ID and one or more unique products of varying quantities that the user has
purchased. Orders should include the date of the order as well as the quantity of each
item purchased (each product will have a different quantity associated with an order, i.e.
5 of item A, 7 of item B, and so on.

Part 2 : 
Application operations

Part 3 :
Test the performance of your application with a number of concurrent threads
ranging from 1-10. (That is, you will test once with one thread, once with two threads, and so
on.) For a period of five minutes, all threads should repeatedly execute one of the operations
you have implemented in a loop. Which operation to perform during each iteration of the loop
should be selected at random according to the following probabilities described.

Provide the code which executes the test above. You should also include plots of the two
recorded values (y-axis) against the number of running threads (x-axis). Briefly explain
your results
