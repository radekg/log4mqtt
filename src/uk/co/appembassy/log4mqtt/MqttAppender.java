package uk.co.appembassy.log4mqtt;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LoggingEvent;
import org.eclipse.paho.client.mqttv3.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class MqttAppender extends AppenderSkeleton implements MqttCallback {

    private MqttClient mqtt;
    private String hostName;
    private String ip;
    private final int RECONNECT_MIN = 1000;
    private final int RECONNECT_MAX = 32000;
    private int currentReconnect = 1000;

    private String broker;
    private String clientid;
    private String username;
    private String password;
    private int connectionTimeout = 2000;
    private int keepAliveInterval = 60000;
    private String topic;
    private int qos = 0;
    private boolean retain = false;
    private String outputFormat = "json";

    public String getBroker() {
        return broker;
    }

    public void setBroker(String broker) {
        this.broker = broker;
    }

    public String getClientid() {
        return clientid;
    }

    public void setClientid(String clientid) {
        this.clientid = clientid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getKeepAliveInterval() {
        return keepAliveInterval;
    }

    public void setKeepAliveInterval(int keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    public boolean isRetain() {
        return retain;
    }

    public void setRetain(boolean retain) {
        this.retain = retain;
    }

    private void connectMqtt() {
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setConnectionTimeout(connectionTimeout);
        opts.setKeepAliveInterval(keepAliveInterval);
        if ( username != null ) {
            opts.setUserName(username);
        }
        if ( password != null ) {
            opts.setPassword(password.toCharArray());
        }
        try {
            mqtt = new MqttClient(broker, clientid, null);
            mqtt.connect(opts);
            currentReconnect = RECONNECT_MIN;
        } catch (MqttSecurityException ex1) {
            errorHandler.error("MQTT Security error: " + ex1);
        } catch (MqttException ex2) {
            int code = ex2.getReasonCode();
            switch (code) {
                case 0: // connection successful
                    currentReconnect = RECONNECT_MIN;
                    break;
                case 1:
                    errorHandler.error("MQTT connection error: Connection Refused: unacceptable protocol version");
                    break;
                case 2:
                    errorHandler.error("MQTT connection error: Connection Refused: identifier rejected");
                    break;
                case 3:
                    errorHandler.error("MQTT connection error: Connection Refused: server unavailable");
                    break;
                case 4:
                    errorHandler.error("MQTT connection error: Connection Refused: bad user name or password");
                    break;
                case 5:
                    errorHandler.error("MQTT connection error: Connection Refused: not authorized");
                    break;
                default:
                    errorHandler.error("MQTT connection error: Unknown response -> " + code);
            }
        }
    }

    private void reconnectMqtt() {
        if ( currentReconnect < RECONNECT_MAX ) {
            currentReconnect += currentReconnect;
        }
        Thread t = new Thread() {
            public void run() {
                try {
                    Thread.sleep(currentReconnect);
                    connectMqtt();
                } catch (InterruptedException ex) {}
            }
        };
    }

    public boolean requiresLayout() { return true; }

    public void activateOptions() {

        if ( outputFormat.equals("json") ) {
            this.setLayout(new JsonLoggingEventLayout());
        } else if ( outputFormat.equals("xml") ) {

        }

        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            hostName = "<unknown>";
        }
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            ip = "<unknown>";
        }

        if ( clientid.indexOf("{ip}") > -1 ) {
            clientid = clientid.replace("{ip}".subSequence(0,"{ip}".length()), ip.subSequence(0,ip.length()));
        } else if ( clientid.indexOf("{hostname}") > -1 ) {
            clientid = clientid.replace("{hostname}".subSequence(0,"{hostname}".length()), hostName.subSequence(0,hostName.length()));
        }

        connectMqtt();
    }

    public synchronized void append( LoggingEvent event ) {

        if( this.layout == null ){
            errorHandler.error("No layout for appender " + name, null, ErrorCode.MISSING_LAYOUT );
            return;
        }

        MqttMessage msg = new MqttMessage();
        msg.setPayload( this.layout.format(event).getBytes() );

        msg.setQos( qos );
        msg.setRetained( retain );
        try {
            if ( mqtt != null ) {
                mqtt.getTopic(this.topic).publish(msg);
            }
        } catch (MqttPersistenceException ex1) {
            errorHandler.error("MQTT Could not send a message: " + ex1);
            errorHandler.error("MQTT: " + msg);
        } catch (MqttException ex2) {
            errorHandler.error("MQTT Could not send a message: " + ex2);
            errorHandler.error("MQTT: " + msg);
        }
    }

    public synchronized void close() {
        try {
            mqtt.disconnect();
        } catch (MqttException ex) {
            errorHandler.error("Could not disconnect the MQTT client: " + ex);
        } finally {
            mqtt = null;
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        this.close();
        errorHandler.error("Connection to the MQTT broker lost, reconnecting: " + cause);
        reconnectMqtt();
    }

    @Override
    public void messageArrived(MqttTopic topic, MqttMessage message) throws Exception {
        // we are not receiving any messages here...
    }

    @Override
    public void deliveryComplete(MqttDeliveryToken token) {
        // we are not receiving any messages here...
    }
}
