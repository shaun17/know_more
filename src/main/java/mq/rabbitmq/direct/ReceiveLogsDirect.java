package mq.rabbitmq.direct;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.springframework.amqp.rabbit.connection.ConnectionFactoryConfigurationUtils;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

public class ReceiveLogsDirect {
	private static final String EXCHANGE = "direct_logs";

	public static void main(String[] args) throws IOException, TimeoutException {
		ConnectionFactory cf = new ConnectionFactory();
		cf.setHost("47.113.102.247");
		Connection newConnection = cf.newConnection();
		Channel createChannel = newConnection.createChannel();
		createChannel.exchangeDeclare(EXCHANGE, "direct");
		String queue = createChannel.queueDeclare().getQueue();
		createChannel.queueBind(queue, EXCHANGE, "routingThree");
		System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

		DeliverCallback dc = (consumerTag, delivery )->{
			String message = new String(delivery.getBody(),"utf-8");
			 System.out.println(" [x] Received '" +
			            delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
		};
		createChannel.basicConsume(queue, true, dc, consumerTag->{});

	}
}
