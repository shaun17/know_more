package mq.rabbitmq.helloworld;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

public class Customer {
	private final static String QUEUE_NAME = "hello";

	public static void main(String[] args) throws IOException, TimeoutException {
		ConnectionFactory cf = new ConnectionFactory();
		cf.setHost("47.113.102.247");
		Connection newConnection = cf.newConnection();
		Channel createChannel = newConnection.createChannel();
		createChannel.queueDeclare(QUEUE_NAME, false, false, false, null);
	    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
	    
	    DeliverCallback dc = (consumerTag,message)->{
	    	String str = new String(message.getBody(),"utf-8");
	        System.out.println(" [x] Received '" + str + "'");
		};
		createChannel.basicConsume(QUEUE_NAME, true, dc, consumerTag->{});
	}
}
