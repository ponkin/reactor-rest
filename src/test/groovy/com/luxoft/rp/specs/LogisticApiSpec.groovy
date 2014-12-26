package com.luxoft.rp.specs

import com.luxoft.rp.Application
import groovyx.net.http.RESTClient
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.test.context.ContextConfiguration
import spock.lang.Ignore
import spock.lang.Specification

import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*

/**
 * Integration tests
 */
@ContextConfiguration(loader = SpringApplicationContextLoader.class, classes = Application.class)
class LogisticApiSpec extends Specification {

    @Ignore
    def "sendMessage should return 200 & a message with the status code S001"() {
        setup:
        def primerEndpoint = new RESTClient('http://localhost:3000/sendMessage')
        when:
        def resp = primerEndpoint.post(
                contentType: XML,
                requestContentType: XML,
                body:  {
                    auth {
                        user 'Bob'
                        password 'pass'
                    }
                }
        )

        then:
        with(resp) {
            status == 200
            contentType == "application/xml"
            data.name() == "SendMessageResult"
            data.Result.Code == "S0001"
        }

    }
}