package com.cooperativeai.statemanagement;

public class MS {

    public static MainStore mainStore;

    public MS(String token,double lat,double lon){
        this.mainStore = new MainStore(token,lat,lon);
    }

}
