package com.luxoft.rp;

import com.luxoft.rp.model.ResultTypeBuilder;
import com.luxoft.rp.model.SendMessageResult;
import io.netty.buffer.ByteBuf;
import kafka.producer.KeyedMessage;
import lombok.extern.slf4j.Slf4j;
import kafka.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.event.Event;
import reactor.function.Function;

import java.util.Properties;

/**
 * Created at 24.12.2014.
 *
 * @author Alexey Ponkin
 * @version 1
 */
@Component
@Slf4j
public class KafkaStorage implements Function<Event<ByteBuf>, SendMessageResult> {

    private final kafka.javaapi.producer.Producer<Integer, Object> producer;
    private final String topic;

    @Autowired
    public KafkaStorage(@Value("${kafka-host}")String kafkaHost, @Value("${kafka-topic}") String topic)  {
        Properties props = new Properties();
        props.put("metadata.broker.list", kafkaHost);
        props.put("serializer.class", "kafka.serializer.DefaultEncoder");
        //props.put("partitioner.class", "example.producer.SimplePartitioner");
        props.put("producer.type", "sync");
        props.put("request.required.acks", "1");

        ProducerConfig config = new ProducerConfig(props);
        producer = new kafka.javaapi.producer.Producer<>(config);
        this.topic = topic;

    }

    @Override
    public SendMessageResult apply(Event<ByteBuf> byteBufferEvent) {
        SendMessageResult result = new SendMessageResult();
        ResultTypeBuilder builder = new ResultTypeBuilder();

        ByteBuf content = byteBufferEvent.getData();
        byte[] bytes = new byte[content.readableBytes()];
        content.readBytes(bytes);
        content.release();
        log.debug("***sendMessage***:{}", new String(bytes));
        producer.send(new KeyedMessage<>(topic, bytes));
        result.setResult(builder.ok().build());
        return result;
    }
}
