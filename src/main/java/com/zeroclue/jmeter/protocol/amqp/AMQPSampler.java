package com.zeroclue.jmeter.protocol.amqp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

public abstract class AMQPSampler extends AbstractSampler implements TestBean {

    public static final int DEFAULT_PORT = 5672;
    public static final String DEFAULT_PORT_STRING = Integer.toString(DEFAULT_PORT);

    public static final int DEFAULT_TIMEOUT = 1000;
    public static final String DEFAULT_TIMEOUT_STRING = Integer.toString(DEFAULT_TIMEOUT);


    private static final Logger log = LoggingManager.getLoggerForClass();


    //++ These are JMX names, and must not be changed
    protected static final String EXCHANGE = "AMQPSampler.Exchange";
    protected static final String EXCHANGE_TYPE = "AMQPSampler.ExchangeType";
    protected static final String QUEUE = "AMQPSampler.Queue";
    protected static final String ROUTING_KEY = "AMQPSampler.RoutingKey";
    protected static final String VIRUTAL_HOST = "AMQPSampler.VirtualHost";
    protected static final String HOST = "AMQPSampler.Host";
    protected static final String PORT = "AMQPSampler.Port";
    protected static final String USERNAME = "AMQPSampler.Username";
    protected static final String PASSWORD = "AMQPSampler.Password";
    private static final String TIMEOUT = "AMQPSampler.Timeout";
    private static final String MESSAGE_TTL = "AMQPSampler.MessageTTL";

    private transient ConnectionFactory factory;
    private transient Connection connection;

    protected AMQPSampler(){
        factory = new ConnectionFactory();
    }

    protected boolean initChannel() throws IOException {
        Channel channel = getChannel();
        if(channel != null && channel.isOpen()){
            return false;
        }
        if(channel != null && !channel.isOpen()){
            log.warn("channel " + channel.getChannelNumber()
                    + " closed unexpectedly: ", channel.getCloseReason());
        }
        factory.setConnectionTimeout(getTimeoutAsInt());
        factory.setVirtualHost(getVirtualHost());
        factory.setHost(getHost());
        factory.setPort(getPortAsInt());
        factory.setUsername(getUsername());
        factory.setPassword(getPassword());

        log.info("RabbitMQ ConnectionFactory using:"
                +"\n\t virtual host: " + getVirtualHost()
                +"\n\t host: " + getHost()
                +"\n\t port: " + getPort()
                +"\n\t username: " + getUsername()
                +"\n\t password: " + getPassword()
                +"\n\t timeout: " + getTimeout()
                );

        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.exchangeDeclare(getExchange(), "direct", true);
        channel.queueDeclare(getQueue(), true, false, false, getQueueArguments());
        channel.queueBind(getQueue(), getExchange(), getRoutingKey());

        log.info("bound to:"
                +"\n\t queue: " + getQueue()
                +"\n\t exchange: " + getExchange()
                +"\n\t routing key: " + getRoutingKey()
                +"\n\t arguments: " + getQueueArguments()
                );


        if(!channel.isOpen()){
            log.fatalError("Failed to open channel: " + channel.getCloseReason().getLocalizedMessage());
        }
        setChannel(channel);
        return true;
    }

    private Map<String, Object> getQueueArguments() {
        Map<String, Object> arguments = new HashMap<String, Object>();

        if(getMessageTTL() != null && !getMessageTTL().isEmpty())
            arguments.put("x-message-ttl", getMessageTTLAsInt());

        return arguments;
    }

    protected abstract Channel getChannel();
    protected abstract void setChannel(Channel channel);

    // TODO: make this configurable
    protected BasicProperties getProperties() {
        AMQP.BasicProperties properties = MessageProperties.PERSISTENT_TEXT_PLAIN;
        return properties;
    }

    /**
     * @return a string for the sampleResult Title
     */
    protected String getTitle() {
        return this.getName();
    }

    /**
     * the implementation calls testEnded() without any parameters.
     */
    public void testEnded(String host) {
        testEnded();
    }

    /**
     * endTest cleans up the client
     *
     * @see junit.framework.TestListener#endTest(junit.framework.Test)
     */
    public void testEnded() {
        log.info("AMQPSampler.testEnded called");
        try {
            //getChannel().close();   // closing the connection will close the channel if it's still open
            if(connection.isOpen())
                connection.close();
        } catch (IOException e) {
            log.error("Failed to close connection", e);
        }
    }

    protected int getTimeoutAsInt() {
        if (getPropertyAsInt(TIMEOUT) < 1) {
            return DEFAULT_TIMEOUT;
        }
        return getPropertyAsInt(TIMEOUT);
    }

    public String getTimeout() {
        return getPropertyAsString(TIMEOUT, DEFAULT_TIMEOUT_STRING);
    }


    public void setTimeout(String s) {
        setProperty(TIMEOUT, s);
    }

    public String getExchange() {
        return getPropertyAsString(EXCHANGE);
    }

    public void setExchange(String name) {
        setProperty(EXCHANGE, name);
    }


    public String getExchangeType() {
        return getPropertyAsString(EXCHANGE_TYPE);
    }

    public void setExchangeType(String name) {
        setProperty(EXCHANGE_TYPE, name);
    }


    public String getQueue() {
        return getPropertyAsString(QUEUE);
    }

    public void setQueue(String name) {
        setProperty(QUEUE, name);
    }


    public String getRoutingKey() {
        return getPropertyAsString(ROUTING_KEY);
    }

    public void setRoutingKey(String name) {
        setProperty(ROUTING_KEY, name);
    }


    public String getVirtualHost() {
        return getPropertyAsString(VIRUTAL_HOST);
    }

    public void setVirtualHost(String name) {
        setProperty(VIRUTAL_HOST, name);
    }


    public String getMessageTTL() {
        return getPropertyAsString(MESSAGE_TTL);
    }

    public void setMessageTTL(String name) {
        setProperty(MESSAGE_TTL, name);
    }

    protected Integer getMessageTTLAsInt() {
        if (getPropertyAsInt(MESSAGE_TTL) < 1) {
            return null;
        }
        return getPropertyAsInt(MESSAGE_TTL);
    }

    public String getHost() {
        return getPropertyAsString(HOST);
    }

    public void setHost(String name) {
        setProperty(HOST, name);
    }


    public String getPort() {
        return getPropertyAsString(PORT);
    }

    public void setPort(String name) {
        setProperty(PORT, name);
    }

    protected int getPortAsInt() {
        if (getPropertyAsInt(PORT) < 1) {
            return DEFAULT_PORT;
        }
        return getPropertyAsInt(PORT);
    }



    public String getUsername() {
        return getPropertyAsString(USERNAME);
    }

    public void setUsername(String name) {
        setProperty(USERNAME, name);
    }


    public String getPassword() {
        return getPropertyAsString(PASSWORD);
    }

    public void setPassword(String name) {
        setProperty(PASSWORD, name);
    }
}
