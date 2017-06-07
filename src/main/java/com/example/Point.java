package com.example;

import java.io.Serializable;

/**
 * Created by akalinichenko on 6/6/17.
 */
public class Point implements Serializable {
    public int id;
    public double latitude;
    public double longitude;
    public double altitude;
    public double latEps;
    public double longEps;
    public double altEps;
}
