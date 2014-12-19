package com.luxoft.rp.api;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import reactor.core.Reactor;
import reactor.event.Event;
import reactor.function.Consumer;
import reactor.net.NetChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicReference;

import static  io.netty.handler.codec.http.HttpHeaders.Names.*;

/**
 * Created at 19.12.2014.
 *
 * @author Alexey Ponkin
 * @version 1
 */
public class DasServiceRestApi {

    public static final String SEND_MESSAGE_URI = "/sendMessage";

    interface FileWriteSuccess{

        void success(Integer result, Object attachment);
    }

    interface FileWriteFailure {

        void failure(Throwable exc, Object attachment);

    }


    public static Consumer<FullHttpRequest> sendMessage(NetChannel<FullHttpRequest, FullHttpResponse> channel,
                                                        AtomicReference<Path> thumbnail,
                                                        Reactor reactor) {
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
                    resp.content().writeBytes("<test>success</test>".getBytes());
                    resp.headers().set(CONTENT_TYPE, "application/xml");
                    resp.headers().set(CONTENT_LENGTH, resp.content().readableBytes());
                    channel.send(resp);

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


