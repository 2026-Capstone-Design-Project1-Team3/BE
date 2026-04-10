package com.server.talkup_be.exception;

//calibration 값이 null일 경우
public class MissingCalibrationException extends RuntimeException {
    public MissingCalibrationException(String message) {
        super(message);
    }
}