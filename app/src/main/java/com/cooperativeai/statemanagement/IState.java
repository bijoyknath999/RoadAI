/*
 * Created by Sujoy Datta. Copyright (c) 2020. All rights reserved.
 *
 * To the person who is reading this..
 * When you finally understand how this works, please do explain it to me too at sujoydatta26@gmail.com
 * P.S.: In case you are planning to use this without mentioning me, you will be met with mean judgemental looks and sarcastic comments.
 */

package com.cooperativeai.statemanagement;

import com.cooperativeai.statemanagement.StateProps.Distress;
import com.cooperativeai.statemanagement.StateProps.GpsLatLon;

import org.immutables.value.Generated;

import java.util.LinkedList;

import javax.annotation.concurrent.Immutable;

@SuppressWarnings("all")
@Generated(from = "Immutables.generator",generator = "State")
@Immutable
public final class IState implements State{

    private final GpsLatLon gps;
    private final LinkedList<Distress> distressList;

    public IState(GpsLatLon gps, LinkedList<Distress> distressList) {
        this.gps = gps;
        this.distressList = distressList;
    }

    @Override
    public GpsLatLon gps() {
        return gps;
    }

    @Override
    public LinkedList<Distress> distressList() {
        return distressList;
    }
}
