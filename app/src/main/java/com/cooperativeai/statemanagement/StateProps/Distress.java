/*
 * Created by Sujoy Datta. Copyright (c) 2020. All rights reserved.
 *
 * To the person who is reading this..
 * When you finally understand how this works, please do explain it to me too at sujoydatta26@gmail.com
 * P.S.: In case you are planning to use this without mentioning me, you will be met with mean judgemental looks and sarcastic comments.
 */

package com.cooperativeai.statemanagement.StateProps;

import com.cooperativeai.statemanagement.StateProps.GpsLatLon;

public class Distress {

    private GpsLatLon gps;
    private String distress = "";
    private int severity = 0;
    private double classScore = 0;
    private BoundingBox boundingbox;

    public Distress(GpsLatLon gps,String distress ,int severity, double classScore,BoundingBox boundingBox){
        this.gps = gps;
        this.distress = distress;
        this.severity = severity;
        this.classScore = classScore;
        this.boundingbox = boundingBox;
    }

    public GpsLatLon getGps() {
        return gps;
    }

    public String getDistress() {
        return distress;
    }

    public int getSeverity() {
        return severity;
    }

    public double getClassScore(){
        return classScore;
    }

    public BoundingBox getBoundingbox() {
        return boundingbox;
    }
}
