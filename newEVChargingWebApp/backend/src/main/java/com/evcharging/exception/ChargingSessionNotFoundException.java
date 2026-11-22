package com.evcharging.exception;

public class ChargingSessionNotFoundException extends RuntimeException {

  public ChargingSessionNotFoundException(String message)
  {
      super(message);
  }

}
