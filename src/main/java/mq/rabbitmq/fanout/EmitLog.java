package mq.rabbitmq.fanout;

import java.util.Scanner;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class EmitLog {
	private static final String EXCHANGE = "log";

	public static void main(String[] args) throws Exception {
		ConnectionFactory cf = new ConnectionFactory();
		cf.setHost("47.113.102.247");
		try (Connection connection = cf.newConnection(); Channel channel = connection.createChannel()) {
			channel.exchangeDeclare(EXCHANGE, "fanout");
			Scanner s = new Scanner(System.in);
			String message = s.nextLine().length() < 2 ? "info: Hello World!" :
				"info2222: Hello22222 World22222!";
			channel.basicPublish(EXCHANGE, "", null, message.getBytes("utf-8"));
	        System.out.println(" [x] Sent '" + message + "'");

		}
	}
}
