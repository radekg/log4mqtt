# Log4j MQTT appender

Usage:

    # output messages into a rolling log file as well as stdout
    log4j.rootLogger=stdout,MQTT

    # stdout
    log4j.appender.stdout=org.apache.log4j.ConsoleAppender
    log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
    log4j.appender.stdout.layout.ConversionPattern=%5p %d{HH:mm:ss,SSS} %m%n

    # MQTT
    log4j.appender.MQTT=uk.co.appembassy.log4mqtt.MqttAppender
    log4j.appender.MQTT.topic=logging/topic
    #log4j.appender.MQTT.username=username
    #log4j.appender.MQTT.password=password
    log4j.appender.MQTT.broker=tcp://localhost:1883
    log4j.appender.MQTT.clientid=clientid{ip}{hostname}
    # the setting below defaults to json
    log4j.appender.MQTT.outputFormat=[json|xml]

Client ID may contain two special values: `{id}` and `{hostname}`. If the ID is `cassandra-{ip}` and appender is working on `127.0.0.1`, the ID will become `cassandra-127.0.0.1`. Alternatively `{hostname}` can be used.

# Feedback

Any ideas? Improvements?