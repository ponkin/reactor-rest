package com.luxoft.rp.model;

/**
 * Created at 23.12.2014.
 *
 * @author Alexey Ponkin
 * @version 1
 */
public class ResultTypeBuilder {
    String code;
    String message;

    public ResultTypeBuilder(){
        super();
    }

    public ResultTypeBuilder ok(){
        code = "S0001";
        return this;
    }

    public ResultTypeBuilder message(String message){
        this.message = message;
        return this;
    }

    public ResultType build(){
        ResultType result = new ResultType();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

}
