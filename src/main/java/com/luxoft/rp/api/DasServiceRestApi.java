package com.luxoft.rp.api;

import com.luxoft.rp.service.LogisticFront;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.function.Consumer;
import reactor.net.NetChannel;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static  io.netty.handler.codec.http.HttpHeaders.Names.*;

/**
 * Created at 19.12.2014.
 *
 * @author Alexey Ponkin
 * @version 1
 */
@Service
public class DasServiceRestApi {

    public static final String SEND_MESSAGE_URI = "/sendMessage";

    interface FileWriteSuccess{

        void success(Integer result, Object attachment);
    }

    interface FileWriteFailure {

        void failure(Throwable exc, Object attachment);

    }
    @Autowired
    public LogisticFront front;

    @Autowired
    private JAXBContext jaxbContext;

    public Consumer<FullHttpRequest> sendMessage(NetChannel<FullHttpRequest, FullHttpResponse> channel) throws JAXBException {
        final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        return req -> {
            if (req.getMethod() != HttpMethod.POST) {
                channel.send(badRequest(req.getMethod() + " not supported for this URI"));
                return;
            }

            // write to a temp file
            Path messageIn = null;
            try {
                messageIn = writeToFile("send-message", req.content(), (Integer result, Object attachment) -> {

                    DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                            HttpResponseStatus.OK);
                    ByteArrayOutputStream out = new ByteArrayOutputStream(256);
                    try {
                        jaxbMarshaller.marshal(front.sendMessage(), out);
                        resp.content().writeBytes(out.toByteArray());
                        resp.headers().set(CONTENT_TYPE, "application/xml");
                        resp.headers().set(CONTENT_LENGTH, resp.content().readableBytes());
                    } catch (JAXBException e) {
                        resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                                HttpResponseStatus.INTERNAL_SERVER_ERROR);
                        resp.content().writeBytes(e.getMessage().getBytes());
                        resp.headers().set(CONTENT_TYPE, "test/plain");
                        resp.headers().set(CONTENT_LENGTH, resp.content().readableBytes());
                    }finally{
                        channel.send(resp);
                    }

                }, (Throwable exc, Object attachment) -> {
                    DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                            HttpResponseStatus.INTERNAL_SERVER_ERROR);
                    resp.headers().set(CONTENT_TYPE, "application/xml");
                    resp.headers().set(CONTENT_LENGTH, resp.content().readableBytes());
                    channel.send(resp);
                });
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }

        };
    }


    private static Path writeToFile(String prefix, ByteBuf content, FileWriteSuccess success, FileWriteFailure failure) throws IOException {
        byte[] bytes = new byte[content.readableBytes()];
        content.readBytes(bytes);
        content.release();

        // write to a temp file
        Path msg = Files.createTempFile(prefix, ".txt");

        AsynchronousFileChannel fileChannel =
                AsynchronousFileChannel.open(msg,  StandardOpenOption.WRITE);

        fileChannel.write(ByteBuffer.wrap(bytes), 0, "Write operation 1", new CompletionHandler<Integer, Object>(){

            @Override
            public void completed(Integer result, Object attachment) {
                success.success(result, attachment);
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                failure.failure(exc, attachment);
            }
        });
        return msg;
    }

    public static Consumer<Throwable> errorHandler(NetChannel<FullHttpRequest, FullHttpResponse> channel){
        return ev -> {
            DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                    HttpResponseStatus.INTERNAL_SERVER_ERROR);
            resp.content().writeBytes(ev.getMessage().getBytes());
            resp.headers().set(CONTENT_TYPE, "application/xml");
            resp.headers().set(CONTENT_LENGTH, resp.content().readableBytes());
            channel.send(resp);
        };
    }

    public static FullHttpResponse badRequest(String msg) {
        DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
        resp.content().writeBytes(msg.getBytes());
        resp.headers().set(CONTENT_TYPE, "application/xml");
        resp.headers().set(CONTENT_LENGTH, resp.content().readableBytes());
        return resp;
    }
}


