package com.luxoft.rp.service;

import com.luxoft.rp.model.ResultTypeBuilder;
import com.luxoft.rp.model.SendMessageResult;
import org.springframework.stereotype.Service;

/**
 * DAS Logistic service implementation
 *
 * Created at 23.12.2014.
 *
 * @author Alexey Ponkin
 * @version 1
 */
@Service
public class LogisticFront {

    /**
     * Операция «отправить сообщение»
     */
    public SendMessageResult sendMessage(){
        SendMessageResult result = new SendMessageResult();
        result.setResult(new ResultTypeBuilder().ok().build());
        return result;
    }
}
