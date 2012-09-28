package uk.co.appembassy.log4mqtt;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.eclipse.paho.client.mqttv3.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;

public class MqttAppender extends AppenderSkeleton implements MqttCallback {

    private MqttClient mqtt;
    private String hostName;
    private String ip;

    private String broker;
    private String clientid;
    private String username;
    private String password;
    private int connectionTimeout = 2000;
    private int keepAliveInterval = 60000;
    private String topic;
    private int qos = 0;
    private boolean retain = false;

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

    public boolean requiresLayout() { return false; }

    public void activateOptions() {

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
        } catch (MqttSecurityException ex1) {
            errorHandler.error("MQTT Security error: " + ex1);
        } catch (MqttException ex2) {
            if ( ex2.getReasonCode() != 0 ) {
                errorHandler.error("MQTT error: " + ex2);
            }
        }
    }

    public synchronized void append( LoggingEvent event ) {

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"hostname\":");
        json.append(",\"ip\":");
        json.append(",\"timestamp\":");
        json.append(event.getTimeStamp());
        json.append(",\"error_level_string\":");
        json.append("\"" + event.getLevel().toString() + "\"");
        json.append(",\"error_level_code\":");
        json.append(event.getLevel().toInt());
        json.append(",\"message\":");
        json.append("\"" + event.getMessage().toString().replaceAll("\"", "\\\"") + "\"");
        json.append(",\"fqn\":");
        json.append("\"" + event.getFQNOfLoggerClass().replaceAll("\"", "\\\"") + "\"");
        json.append(",\"logger_name\":");
        json.append("\"" + event.getLoggerName().replaceAll("\"", "\\\"") + "\"");
        if ( event.getNDC() != null ) {
            json.append(",\"ndc\":");
            json.append("\"" + event.getNDC().replaceAll("\"", "\\\"") + "\"");
        }
        if ( event.getRenderedMessage() != null ) {
            json.append(",\"rendered_message\":");
            json.append("\"" + event.getRenderedMessage().replaceAll("\"", "\\\"") + "\"");
        }
        if (event.locationInformationExists()) {
            json.append(",\"location_info\":");
            json.append("\"" + event.getLocationInformation().fullInfo.replaceAll("\"", "\\\"") + "\"");
        }
        if ( event.getThreadName() != null ) {
            json.append(",\"thread_name\":");
            json.append("\"" + event.getThreadName().replaceAll("\"", "\\\"") + "\"");
        }
        if ( event.getThrowableStrRep() != null ) {
            String[] throwable = event.getThrowableStrRep();
            if ( throwable.length > 0 ) {
                json.append(",\"throwable\":[");
                for ( int i=0; i<throwable.length; i++ ) {
                    if ( i > 0 ) json.append(",");
                    json.append("\"" + throwable[i].replaceAll("\"", "\\\"") + "\"");
                }
                json.append("\"]\"");
            }
        }

        if ( event.getProperties() != null && event.getProperties().size() > 0 ) {
            json.append(",\"properties\":{");
            Iterator<String> iter = event.getProperties().keySet().iterator();
            int c = 0;
            while (iter.hasNext()) {
                if ( c > 0 ) json.append(",");
                String key = iter.next();
                json.append("\"" + key.replaceAll("\"", "\\\"") + "\":");
                json.append("\"" + event.getProperty(key).replaceAll("\"", "\\\"") + "\"");
            }
            json.append("}");
        }

        json.append("}");

        MqttMessage msg = new MqttMessage();
        msg.setPayload( json.toString().getBytes() );
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
