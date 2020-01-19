package mq.rabbitmq.workqueues;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

public class NewTask {
	private static final String QUEUE_NAME = "work";

	public static void main(String[] args) throws IOException, TimeoutException {
		ConnectionFactory cf = new ConnectionFactory();
		cf.setHost("47.113.102.247");
		try (Connection newConnection = cf.newConnection(); Channel channel = newConnection.createChannel()) {
			channel.queueDeclare(QUEUE_NAME, true, false, false, null);// 消息持久化
			String message = String.join(" ", "this is work message");
			channel.basicPublish("", QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes("UTF-8")); //消息持久化
		} catch (Exception e) {
		}
	}

}
