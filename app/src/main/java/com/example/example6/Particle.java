package com.example.example6;

import android.provider.Telephony;

public class Particle {

    //needed for crossing objects
    private double prev_x;
    private double prev_y;


    private double x;
    private double y;
    private double distance;
    private double rotation;

    public Particle(double x, double y, double dist, double rot) {
        this.x = x;
        this.y = y;
        this.distance = dist;
        this.rotation = rot;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double get_prev_X() {
        return this.prev_x;
    }

    public double get_prev_Y() {
        return this.prev_y;
    }

    public double getDistance() { return this.distance;}

    public void updateDistance(double distance, double rotation) {
        this.prev_x = x;
        this.prev_y = y;

        this.distance += distance;
        this.rotation = rotation;
        double radians = Math.toRadians(rotation);
//        System.out.println("Calculated Radians: " + radians + ", used rotation " + rotation);
        this.y -= distance * Math.cos(radians);
        this.x += distance * Math.sin(radians);
    }


}