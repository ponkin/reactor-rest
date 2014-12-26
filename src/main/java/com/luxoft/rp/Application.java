package com.luxoft.rp;

/**
 * Created at 19.12.2014.
 *
 * @author Alexey Ponkin
 * @version 1
 */

import com.luxoft.rp.api.DasServiceRestApi;
import com.luxoft.rp.model.SendMessageResult;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.core.composable.Stream;
import reactor.core.spec.Reactors;
import reactor.event.Event;
import reactor.function.Function;
import reactor.net.NetServer;
import reactor.net.config.ServerSocketOptions;
import reactor.net.netty.NettyServerSocketOptions;
import reactor.net.netty.tcp.NettyTcpServer;
import reactor.net.tcp.spec.TcpServerSpec;
import reactor.spring.context.config.EnableReactor;


import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.util.concurrent.CountDownLatch;

import static reactor.event.selector.Selectors.$;


/**
 * Entry point
 */
@EnableAutoConfiguration
@Configuration
@ComponentScan
@EnableReactor
@Slf4j
public class Application implements CommandLineRunner {

    @Value("${http-port}")
    public int port;

    @Value("${http-host}")
    public String host;

    @Autowired
    private Function<Event<ByteBuf>, SendMessageResult> messageStorage;

    @Bean
    public Reactor reactor(Environment env) {
        Reactor reactor = Reactors.reactor(env, Environment.RING_BUFFER);
        reactor.receive($("save"), messageStorage);
        return reactor;
    }

    @Bean
    public JAXBContext jaxbContext() throws JAXBException {
        return JAXBContext.newInstance("com.luxoft.rp.model");
    }

    @Autowired
    DasServiceRestApi api;


    @Bean
    public NetServer<FullHttpRequest, FullHttpResponse> restApi(Environment env,
                                                                ServerSocketOptions opts,
                                                                Reactor reactor,
                                                                CountDownLatch closeLatch) throws InterruptedException {

        NetServer<FullHttpRequest, FullHttpResponse> server = new TcpServerSpec<FullHttpRequest, FullHttpResponse>(
                NettyTcpServer.class)
                .env(env).dispatcher("sync").options(opts).listen(host, port)
                .consume(ch -> {
                    // filter requests by URI via the input Stream
                    Stream<FullHttpRequest> in = ch.in();

                    in.filter((FullHttpRequest req) -> DasServiceRestApi.SEND_MESSAGE_URI.equals(req.getUri()))
                            .when(Throwable.class, DasServiceRestApi.errorHandler(ch))
                            .consume(api.sendMessage(ch, reactor));


                    // shutdown this app
                    in.filter((FullHttpRequest req) -> "/shutdown".equals(req.getUri()))
                            .consume(req -> closeLatch.countDown());
                })
                .get();

        server.start().await();

        return server;
    }


    @Bean
    public ServerSocketOptions serverSocketOptions() {
        return new NettyServerSocketOptions()
                .pipelineConfigurer(pipeline -> pipeline.addLast(new HttpServerCodec())
                        .addLast(new HttpObjectAggregator(16 * 1024 * 1024)));
    }


    @Override
    public void run(String... strings) throws Exception {
        log.debug("Start listening for incoming requests");
    }

    @Bean
    public CountDownLatch closeLatch() {
        return new CountDownLatch(1);
    }

    public static void main(String... args) throws InterruptedException {
        ApplicationContext ctx = SpringApplication.run(Application.class, args);

        // Reactor's TCP servers are non-blocking so we have to do something to keep from exiting the main thread
        CountDownLatch closeLatch = ctx.getBean(CountDownLatch.class);
        closeLatch.await();
    }

}
