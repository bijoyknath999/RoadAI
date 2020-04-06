/*
 * Created by Sujoy Datta. Copyright (c) 2020. All rights reserved.
 *
 * To the person who is reading this..
 * When you finally understand how this works, please do explain it to me too at sujoydatta26@gmail.com
 * P.S.: In case you are planning to use this without mentioning me, you will be met with mean judgemental looks and sarcastic comments.
 */

package com.cooperativeai.statemanagement.StateProps;

public class BoundingBox {

    private double left;
    private double top;
    private double right;
    private double bottom;

    public BoundingBox(double left,double right,double top,double bottom){
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
    }
}
