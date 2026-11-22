package com.evcharging.exception;

public class ChargingStationNotFoundException  extends RuntimeException{

    public ChargingStationNotFoundException (String meassge)
    {
        super(meassge);
    }
}
