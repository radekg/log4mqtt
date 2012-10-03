package uk.co.appembassy.log4mqtt;

import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: rad
 * Date: 29/09/2012
 * Time: 14:25
 * To change this template use File | Settings | File Templates.
 */
public class XmlLoggingEventLayout extends Layout {

    private String hostname;
    private String ip;

    public void activateOptions() {
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            hostname = "<unknown>";
        }
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            ip = "<unknown>";
        }
    }

    public boolean ignoresThrowable() { return true; }

    public String format(LoggingEvent event) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\"?><event>");
        xml.append("<hostname>" + hostname + "</hostname>");
        xml.append("<ip>" + ip + "</ip>");
        xml.append("<timestamp>" + event.getTimeStamp() + "</timestamp>");

        xml.append("<error_level_string><![CDATA[" + event.getLevel().toString() + "]]></error_level_string>");
        xml.append("<error_level_code>" + event.getLevel().toInt() + "</error_level_code>");
        xml.append("<message><![CDATA[" + event.getMessage().toString().replaceAll("\"", "\\\\\"") + "]]></message>");
        xml.append("<fqn><![CDATA[" + event.getFQNOfLoggerClass().replaceAll("\"", "\\\\\"") + "]]></fqn>");
        xml.append("<logger_name><![CDATA[" + event.getLoggerName().replaceAll("\"", "\\\\\"") + "]]></logger_name>");
        if ( event.getNDC() != null ) {
            xml.append("<ndc><![CDATA[" + event.getNDC().replaceAll("\"", "\\\\\"") + "]]></ndc>");
        }
        if ( event.getRenderedMessage() != null ) {
            xml.append("<rendered_message><![CDATA[" + event.getRenderedMessage().replaceAll("\"", "\\\\\"") + "]]></rendered_message>");
        }
        if (event.locationInformationExists()) {
            xml.append("<location_info><![CDATA[" + event.getLocationInformation().fullInfo.replaceAll("\"", "\\\\\"") + "]]></location_info>");
        }
        if ( event.getThreadName() != null ) {
            xml.append("<thread_name><![CDATA[" + event.getThreadName().replaceAll("\"", "\\\\\"") + "]]></thread_name>");
        }
        if ( event.getThrowableStrRep() != null ) {
            String[] throwable = event.getThrowableStrRep();
            if ( throwable.length > 0 ) {
                xml.append("<throwable>");
                for ( int i=0; i<throwable.length; i++ ) {
                    xml.append("<"+i+"><![CDATA[" + throwable[i].replaceAll("\"", "\\\\\"") + "]]></"+i+">");
                }
                xml.append("</throwable>");
            }
        }

        if ( event.getProperties() != null && event.getProperties().size() > 0 ) {
            xml.append("<properties>");
            Iterator<String> iter = event.getProperties().keySet().iterator();
            int c = 0;
            while (iter.hasNext()) {
                String key = iter.next();
                xml.append("<"+key.replaceAll("\"", "\\\\\"")+">");
                xml.append("<![CDATA[" + event.getProperty(key).replaceAll("\"", "\\\\\"") + "]]>");
                xml.append("</"+key.replaceAll("\"", "\\\\\"")+">");
            }
            xml.append("</properties>");
        }

        xml.append("</event>");
        return xml.toString();
    }
}
