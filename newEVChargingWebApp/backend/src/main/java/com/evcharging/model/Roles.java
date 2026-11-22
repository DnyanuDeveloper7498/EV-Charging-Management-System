package com.evcharging.model;

import java.util.Set;

public final class Roles {

    //Main Roles
    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";
    public static final String STATION_OWNER = "STATION_OWNER";


    // Department Admin roles
    public static final String USER_ADMIN = "USER_ADMIN";
    public static final String VEHICLE_ADMIN = "VEHICLE_ADMIN";
    public static final String CHARGING_ADMIN = "CHARGING_ADMIN";
    public static final String FINANCE_ADMIN = "FINANCE_ADMIN";
    public static final String SUPPORT_ADMIN = "SUPPORT_ADMIN";
    public static final String STATION_ADMIN = "STATION_ADMIN";



    public static final Set<String> ALL = Set.of(USER, STATION_OWNER);

    private Roles() {
    }
}
