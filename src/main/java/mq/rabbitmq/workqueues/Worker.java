package mq.rabbitmq.workqueues;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

public class Worker {
	private static final String QUEUE_NAME = "work";

	public static void main(String[] args) throws IOException, TimeoutException {
		ConnectionFactory cf = new ConnectionFactory();
		cf.setHost("localhost");
		Connection connection = cf.newConnection();
		Channel channel = connection.createChannel();
		channel.queueDeclare(QUEUE_NAME, false, false, false, null); //持久化
		System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
		channel.basicQos(1);//一个消费者一次值分一个消息
		DeliverCallback callback = ( consumerTag,  delivery)->{
			String message = new String(delivery.getBody(),"utf-8");
	        System.out.println(" [x] Received '" + message + "'");
	        try {
	            doWork(message);
	          } catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
	            System.out.println(" [x] Done");
	          }
		};
		//hannel.basicConsume(QUEUE_NAME, true, callback, consumerTag->{}); 关闭自动校验，消费者消费消息，给exchange返回ack，
		//然后exchange就可以删除消息，如果由于消费者没有返回ack，则exchange会将消息再转发
		//boolean aotoACK = false;

		channel.basicConsume(QUEUE_NAME, false, callback, consumerTag->{});
		
	}

	private static void doWork(String task) throws InterruptedException {
		for (char ch : task.toCharArray()) {
			if (ch == '.')
				Thread.sleep(1000);
		}
	}
}
