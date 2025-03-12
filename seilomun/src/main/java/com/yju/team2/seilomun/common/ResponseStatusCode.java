package com.yju.team2.seilomun.common;

public class ResponseStatusCode {

    public static final int OK = 200;
    public static final int URL_NOT_FOUND = 404;
    public static final int EMAIL_NOT_VERIFIED = 410;
    public static final int WRONG_PARAMETER = 420;
    public static final int LOGIN_FAILED = 430;
    public static final int SERVER_ERROR = 500;

    private ResponseStatusCode() {
    }

}
