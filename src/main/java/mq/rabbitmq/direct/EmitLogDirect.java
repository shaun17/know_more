/**
 * 
 */
/**
 * @author admin
 *
 */
package mq.rabbitmq.direct;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class EmitLogDirect {
	
	private static final String EXCHANGE="direct_logs";
	public static void main(String[] args) {
		ConnectionFactory cf = new ConnectionFactory();
		cf.setHost("47.113.102.247");
		try(Connection connection = cf.newConnection(); Channel channel = connection.createChannel()){
			channel.exchangeDeclare(EXCHANGE, "direct");
			String message = " this is direct message";
			String routing = "routingTwo";
			channel.basicPublish(EXCHANGE, routing, null, message.getBytes("utf-8"));
	        System.out.println(" [x] Sent '" + routing + "':'" + message + "'");
		}catch (Exception e) {
			
		}
	}
	
}