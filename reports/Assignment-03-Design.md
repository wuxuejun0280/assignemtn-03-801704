# Part-1 Design for streaming analytics
## 1. Description of dataset, streaming analytics and batch analytics

The dataset we pick is gonna be about the yellow taxi in New York city. It contains information about the vender id, drop off location, pick up location, drop of timestamp, pick up timestamp, distance, price, etc. The detailed description and sample dataset is in the `data/` folder. In the design of the platform we assume not all taxies can upload the data of trip right after the trip end. Great amount of data may delay for days because many taxi have to upload data stored locally manually. 

In the streaming analytics we will perform on the dataset will be recording the amount of pick up per location every one hour. We will use the process time to divide the message into different partitions and find out the interval of pickup time(event time). Then we will return the amount of pick up per hour per location. For example, between 17:00 and 18:00 the interval of pick up time is 16:00 to 18:00. Then we will count the amount of pick up per location from 16:00 to 17:00 and from 17:00 to 18:00 and return to user. 

In the batch analytics we will find out the average amount of pick up at that specific time in a year per location. For example, the batch should output data with this schema:`month, day, week, start time, end time, average pick up`. Also we can find out the average distance and average expense per pick up at specific time and location. The reason we do this in batch analytics is because the taxi data doesn't necessarily arrive right after the drop off. So we have to calculate the average everytime the stream analytics indicate more data in that timeinterval is processed and we have to calculate the average of that time interval again. 

## 2. Regarding the key of data streams and delivery guarantees.
First, the analytics should handle keyed data streams. It's because the only with keyed window flink can execute the window computation in parallel. If we use non-keyed window, all the data will come in as one data stream in one task which can increase the latency of streaming anlysis. 

Second, the best delivery guarantees should be exactly once to maintain the integrity of data. However since some data is sent through network from taxies, a good network condition cannot be garanteed. If we cannot garantee the data to be processed exactly once, I think we should guarantee the data to be delivered at least once. Since some region has bad network condition, the chance the data fails to reach message queue is higher. If we use the at-most-once delivery guarantee, the amount pick up at certain location can be biased because lots of data is lost. For a taxi company this outcome will decrease the revenue and bring unconvient to people in certain location. So we will prefer to choose either exactly-once or at-least-once delivery guarantees depend on situation. If it's not possible to use exactly-once delivery guarantees, we will use at-least-once delivery guarantees. 

## 3. Regarding choice of time and window
First, the time we are gonna use is the process time. Idealy we will use the event time but since the delay of data can vary from minutes to days, it is hard to actually implement this. In flink there is a maximum delay for event time. If we set the delay very large, the streaming data won't be able to return on time. Thus here we are gonna use the process time to partition the data into different window and perform analysis on event time. 

Second, we are gonna use tumbling windows for analytics. In stream analysis we are tring to get the amount of pick up every one hour so we are choosing tumbling windows. If we need a average in a time period, we can use sliding window. However in this design that's the job of batch analysis. The seesion windows is not really applicable here because there won't be periodical gap between cluster of data. 

## 4. About the performance matices. 
The throughput is the most important performance matices in this case. That's because we need to make sure the queue of message borker won't keep increase and lost data from the taxi. The latency is relatively less important since the customer probably can accept delay within 10 seconds. The possible use of the data is to manage the taxi distribution around the city and a 10 second delay won't have big impact on the customer since the relocation of taxi probably takes more than 10 minutes. Also because we cannot ensure the network condition, the latency can depend more on the network condition which is impossible to improve through the design of the platform. 

## 5. The design of stream analytics

![](https://github.com/wuxuejun0280/assignemtn-03-801704/blob/master/reports/assignement3.png "design")

The datasource here can be a app of the taxi terminal or a client somewhere taxi can upload data stored locally in terminal. The message broker here is rabbitmq. Then the *clientstreamapp* will reterive the data from *input queue*, perform the analysis and send the data to the *output queue*. The *stream computing service* give api for customer to submit the *clientstreamapp*. It is also in charge of the lifecycle of each *clientstreamapp*. We could have multiple *clientstreamapp* in parallel with *input queue* and *output queue* for each of them.Here we use Rabbitmq because it make sure all the data reterived must be in order. Also the queue persist the message before it is processed, which gives the system more stability. We are gonna use Flink. The main reason is because the learning curve for spark is more steep which will bring inconvenience to customer when they are developing *clientstreamapp*. Also it seems Flink integrate batch analytics and stream analytics much better through better memory management. 

In the implementation of the design we are gonna modify a little bit on the **clientstreamuploadapp** in assignment 2 as the testing data source. We are not gonna implement a *stream computing service* to manage the upload of *clientstreamapp*. To test the *clientstreamapp* we will start the task manually. 

# part-2 Implementation of stream analytics
## 1. The datastructure of message in broker
The message is sent to the broker in form of `columnname:value columnname:value .....`Here is a example message in input broker
`DOLocationID:148 RatecodeID:1 fare_amount:7.5 tpep_dropoff_datetime:2019-02-01 00:03:58 congestion_surcharge:0 VendorID:2 passenger_count:1 tolls_amount:0 improvement_surcharge:0.3 trip_distance:1.5 store_and_fwd_flag:N payment_type:1 total_amount:10.56 extra:0.5 tip_amount:1.76 mta_tax:0.5 tpep_pickup_datetime:2019-01-31 23:55:34 PULocationID:107 ,1575006319309`

The message in the output borker is in form of `month-date-hour-pickup location,amount of pickup`Here are some example output message. 
```
01-31-23-107,2
01-31-23-170,1
02-01-00-246,1
02-01-00-79,1
```
When we get the input message, we split the message by space and assign the required value to a pojo. When we process the list of pojo received in the time interval, we count the number of pickup per location and assign it to a map. After that we compose the output meesage from the map and send the message to the output queue. 

## 2. The key logic of functions. 
First we use the flatmap operator to map the message from input to pojo in datastream. Then we assign the pickup location as the key. After that we set the timewindow to be 5 seconds. The time window is short for test purposes. Then we call the process function to process the data we received in that 5 seconds and seralize the data into string. At last we output the result to message broker and std. 

The TaxiKeySelector is the selector to select the key. It implements KeySelector and return the pickup location as the key. 

The BTSparser parse the string we get from input queue to pojo. In the function we deseralize the message by split the message by space and assign the correlated data to a new pojo and collect it with collector. 

The MyProcessWindowFunction analysis the data, count the amount of pickup per location per time and seralize the data into a string. 

## 3. The test environment and performance
Here we wrote a program read from csv file and send message to message broker to simulate the datasource of client. Since everyting is tested locally, we send a message every 50 ms to the message broker. In the original test we set Parallelism to 2 to perform the test. Everything is tested locally. We are gonna use the throughput of queue from Rabbitmq to measure the performance of **clientstreamapp**.

Here is the output of the log. 
```
2> 01-31-23-100,1
1> 02-01-00-132,3
7> 02-01-00-189,1
7> 01-31-23-48,1
7> 02-01-00-162,1
1> 02-01-00-264,1
1> 02-01-00-107,2
2> 01-31-23-114,1
2> 02-01-00-236,1
6> 02-01-00-246,3
6> 02-01-00-229,2
6> 02-01-00-163,1
5> 02-01-00-79,9
5> 02-01-00-4,1
5> 02-01-00-249,7
4> 02-01-00-234,5
4> 02-01-00-161,4
4> 02-01-00-141,2
3> 02-01-00-255,2
3> 02-01-00-113,3
3> 02-01-00-138,1
8> 02-01-00-90,1
8> 02-01-00-230,4
2> DOLocationID:107 RatecodeID:1 fare_amount:5 tpep_dropoff_datetime:2019-02-01 00:26:33 congestion_surcharge:0 VendorID:2 passenger_count:2 tolls_amount:0 improvement_surcharge:0.3 trip_distance:0.68 store_and_fwd_flag:N payment_type:1 total_amount:7.56 extra:0.5 tip_amount:1.26 mta_tax:0.5 tpep_pickup_datetime:2019-02-01 00:22:07 PULocationID:79 ,1575007449970
1> 02-01-00-186,3
1> 02-01-00-211,1
8> 02-01-00-148,1
5> 02-01-00-256,1
3> 02-01-00-142,1
6> 02-01-00-89,1
4> 02-01-00-158,2
2> 02-01-00-112,1
2> 02-01-00-137,6
7> 02-01-00-75,1
```
Regarding the performance, the throughput is stable at 20/s for rabbitmq. Probably the presure from the ingestion is not enough so the throughput is really stable. 

## 4. About wrong message
In the test we make the client send a predefined message with wrong format with a certain percentage. At first since we didn't mitigate wrong message, the **clientstreamapp** reported a exception and crushed. Then we edited the flatmap function to skip the message with wrong format. After that the job is running smoothly and the performance is still 20/s. The performance here won't change a lot becuase we are just skipping the message with the wrong format. It should not impact the performance at all. 

## 5. About parallelism setting
Here in order to test the throughput of the **clientstreamapp**, we first put 199,998 message into the message queue and run the analytics with different parallelism setting. We set the parallelism to 1, 2, 3 and 20. The through put of the message is always 30000/s at max. I think the reason we cannot observe the difference is because the workload of stream analysis is too light here and even if the stream analysis is not running in parallel the through put has reached the maximum through put of Rabbitmq. The other possibility is that there are certain bottle neck in the program that's limiting the performance of the analytics. 

# part-3 Connection
## 1. Ingest data into coredms. 
![](https://github.com/wuxuejun0280/assignemtn-03-801704/blob/master/reports/coredm.png "design")

If I want the analytic result to store in coredm, I will let the stream analytic app to put message in two message queue. One is for user to reterive and another is for another stream ingestion component to ingest the data into the coredms. We won't let analytic app to do the task because the interaction with database can be time consuming. We don't want that become the bottle neck of the performance of analytic app. We want user to reterive the result from message broker instead of database because when the stream ingestion is taking place, the stream ingestion app may applying update to the database. With mongodb the update lock per document can be annoying in this case. So we want customer to reterive the data from message broker to ensure the accesibility of database. 

## 2. 
