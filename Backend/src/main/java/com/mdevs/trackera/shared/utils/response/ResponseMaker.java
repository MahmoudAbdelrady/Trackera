package com.mdevs.trackera.shared.utils.response;

public class ResponseMaker {
    private final static TrackeraResponse trackeraResponse = new TrackeraResponse();

    public static TrackeraResponse makeResponse(String message, Object data) {
        trackeraResponse.setMessage(message);
        trackeraResponse.setData(data);
        return trackeraResponse;
    }
}
