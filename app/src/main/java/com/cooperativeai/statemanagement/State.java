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

import org.immutables.value.Value;

import java.util.LinkedList;

@Value.Immutable
public interface State {

    GpsLatLon gps();
    LinkedList<Distress> distressList();

}
