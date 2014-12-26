package com.luxoft.rp.api;

import io.netty.handler.codec.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.Reactor;
import reactor.event.Event;
import reactor.function.Consumer;
import reactor.net.NetChannel;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

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

    private final JAXBContext jaxbContext;

    @Autowired
    public DasServiceRestApi(JAXBContext jaxbContext){
        this.jaxbContext = jaxbContext;
    }


    public Consumer<FullHttpRequest> sendMessage(NetChannel<FullHttpRequest, FullHttpResponse> channel, Reactor reactor) {
        return req -> {
            if (req.getMethod() != HttpMethod.POST) {
                channel.send(badRequest(req.getMethod() + " not supported for this URI"));
                return;
            }

            reactor.sendAndReceive("save", Event.wrap(req.content()), eq -> {
                DefaultFullHttpResponse resp = null;
                   try(ByteArrayOutputStream out = new ByteArrayOutputStream(256)) {
                       Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
                       resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                               HttpResponseStatus.OK);
                       jaxbMarshaller.marshal(eq.getData(), out);
                       resp.content().writeBytes(out.toByteArray());
                       resp.headers().set(CONTENT_TYPE, "application/xml");
                       resp.headers().set(CONTENT_LENGTH, resp.content().readableBytes());
                   } catch (JAXBException | IOException e) {
                       resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                               HttpResponseStatus.INTERNAL_SERVER_ERROR);
                       resp.content().writeBytes(e.getMessage().getBytes());
                       resp.headers().set(CONTENT_TYPE, "text/plain");
                       resp.headers().set(CONTENT_LENGTH, resp.content().readableBytes());
                   }finally{
                       channel.send(resp);
                   }
            });

        };
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


