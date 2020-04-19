package com.cooperativeai.statemanagement;

public class MS {

    public static MainStore mainStore;

//    public  MS(String token,double lat,double lon){
////        mainStore = new MainStore(token,lat,lon);
////    }

    public static void setMainStore(MainStore mainstore){
        mainStore = mainstore;
    }

}
