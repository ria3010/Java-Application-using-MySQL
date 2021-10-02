In order to run this application, an additional jar file using Script Runner will be required. This avoids delay in initializing the database. 
Instead of initalizing the database each time it fetches the data from the sql dump file, thereby improving the running speed of the application.


Question : For each test (each different number of threads), you should record the following:
			● Percentage of products with a stock level less than zero at the end of the test
			  (Hint: If your SubmitOrder operation consistently fails to process orders without available
			   stock, what would you expect this value to be?)
			● Total number of operations performed by all threads combined
			  Provide the code which executes the test above. You should also include plots of the two
			  recorded values (y-axis) against the number of running threads (x-axis). 
			  Briefly explain your results.
			  
			  
Answer : ● We can see from the plots provided that the percentage of products with a stock level less than zero would always 
		  remain zero and never go below it because we are ensuring this with the help of a serializable isolation level.
		  
		 ● The total number of operations combined by all threads is : 60,811. Number of operations by each thread is : 
			(Output from application given as below:)
			Hashmap Entries :1 40574
			Hashmap Entries :2 11672
			Hashmap Entries :3 2910
			Hashmap Entries :4 1560
			Hashmap Entries :5 1189
			Hashmap Entries :6 944
			Hashmap Entries :7 774
			Hashmap Entries :8 553
			Hashmap Entries :9 318
			Hashmap Entries :10 317

		As the number of threads increase, we see a decline in the total number of operations performed by each thread.	I believe this is because as we increase
		number of threads, the task is distributed among multiple threads, and each thread performs little work which can increase the overhead of the application.