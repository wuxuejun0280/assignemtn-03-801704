package clientstreamingestapp.rabbitmq;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Queue;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;

public class RabbitmqClient {

	Connection conn;
	Channel channel;

	Queue<String> queue;
	String queueName;

	public RabbitmqClient(String queueName) {
		ConnectionFactory factory = new ConnectionFactory();
		try {
			conn = factory.newConnection();
			factory.setUsername("admin");
			factory.setPassword("password");
			channel = conn.createChannel();
			this.queueName = queueName;
			channel.queueDeclare(queueName, true, false, false, null);
//			DeliverCallback deliverCallback = (consumerTag, delivery) -> {
//				String message = new String(delivery.getBody(), "UTF-8");
//				System.out.println(message);
//				queue.add(message);
//			};
//			channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
//			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void publish(String message) {
		try {
			channel.basicPublish("", queueName, null, message.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
