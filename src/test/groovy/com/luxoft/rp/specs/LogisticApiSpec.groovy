package com.luxoft.rp.specs

import com.luxoft.rp.Application
import groovyx.net.http.RESTClient
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*

@ContextConfiguration(loader = SpringApplicationContextLoader.class, classes = Application.class)
class LogisticApiSpec extends Specification {
    def "sendMessage should return 200 & a message with the status code S001"() {
        setup:
        def primerEndpoint = new RESTClient('http://localhost:3000/sendMessage')
        when:
        def resp = primerEndpoint.request(POST, XML) {

            body = {
                // Arbitrary xml content
                message {
                    sender 'Bob'
                    text 'test'
                }
            }
        }

        then:
        with(resp) {
            status == 200
            contentType == "application/xml"
            data == 'S0001'
        }
    }
}