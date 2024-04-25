package com.example.novelcastserver.bean;

import lombok.Getter;

@Getter
public enum ResultEnum {

    SUCCESS("0000", "Good Job~"),
    FAILURE("9999", "我也不知道哪里出了问题~"),
    PARAMETER_ERROR("0001", "总感觉哪里怪怪的，好像少了点什么~"),
    DATA_NOT_EXISTS("0002", " 我找了八百遍都没找到~"),
    DATA_EXISTS("0003", "这个我有了，我就不要啦~"),
    illegal_token("50008", "认证信息已失效，请重新登录~"),
    other_clients_login("50012", "Other clients logged in"),
    Token_expired("50014", "认证信息已失效，请重新登录~"),
    ;


    private final String code;

    private final String msg;

    ResultEnum(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
