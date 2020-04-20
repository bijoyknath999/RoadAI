
package com.cooperativeai.statemanagement;



import android.location.Location;
import android.util.Log;

import com.cooperativeai.communication.SocketConnection;
import com.cooperativeai.statemanagement.StateProps.Distress;
import com.cooperativeai.statemanagement.StateProps.GpsLatLon;
import com.cooperativeai.utils.Constants;
import com.google.gson.Gson;

import java.util.Iterator;
import java.util.LinkedList;

import trikita.jedux.Action;
import trikita.jedux.Store;

public final class MainStore{

    private Store<Action<StateAction,?>, State> store;
    private SocketConnection socketConnection;
    private Gson gson = new Gson();

    //Actions
    enum StateAction {
        UPDATE_GPS,
        ADD_DISTRESS,
        ADD_DISTRESS_LIST,
        UPDATE_DISTRESS
    }

    public MainStore(String token, double lat, double lon) {

        this.socketConnection = new SocketConnection(this, token);

        //Initializing Gps
        GpsLatLon gps = new GpsLatLon(lat,lon);

        //Initializing Distress List
        LinkedList<Distress> distressesList = new LinkedList<Distress>();

        State state = ImmutableState.builder()
                .gps(gps)
                .distressList(distressesList)
                .build();

        //Initializing State
        this.store = new Store<>(this::reduce, state, new Logger<>("State"));

    }

    //Reducer
    public State reduce(Action<StateAction, ?> action,State old){
        switch (action.type) {
            case UPDATE_GPS: {
                return  ImmutableState.builder()
                            .gps((GpsLatLon) action.value)
                            .distressList((LinkedList<Distress>) old.distressList().clone())
                            .build();
            }
            case ADD_DISTRESS:{
                LinkedList<Distress> distressList = (LinkedList<Distress>) old.distressList().clone();
                distressList.add((Distress) action.value);
                return ImmutableState.builder()
                        .gps(old.gps())
                        .distressList(distressList)
                        .build();
            }
            case ADD_DISTRESS_LIST:{
                LinkedList<Distress> distressList = (LinkedList<Distress>) action.value;
                return ImmutableState.builder()
                        .gps(old.gps())
                        .distressList(distressList)
                        .build();
            }
            case UPDATE_DISTRESS:{
                Distress newDistress = (Distress) action.value;
                LinkedList<Distress> newDistressList = new LinkedList<Distress>();
                LinkedList<Distress> oldDistressList = old.distressList();
                for(Iterator i = oldDistressList.iterator(); i.hasNext();){

                    Distress d = (Distress) i.next();

                    if(d.getGps().getLat() == newDistress.getGps().getLat() && d.getGps().getLon() == newDistress.getGps().getLon()){
                        newDistressList.add(newDistress);
                    }else{
                        newDistressList.add(d);
                    }
                }
                return ImmutableState.builder()
                        .gps(old.gps())
                        .distressList(newDistressList)
                        .build();
            }
        }
        return old;
    }

    public void updateGps(double lat, double lon){
        if(this.socketConnection.getAuthStatus()) {
            store.dispatch(new Action<>(StateAction.UPDATE_GPS, new GpsLatLon(lat, lon)));
            socketConnection.getSocket().emit("GPS_UPDATE", gson.toJson(new GpsLatLon(lat, lon)));
        }
    }

    public void getDataForMap(){
        this.socketConnection.getSocket().emit("GET_MAP_DATA");
    }

    public void addDistress(Distress distress){
        float[] results = new float[1];
        System.out.println("Works ok");
        boolean isPresent = false;
        boolean toUpdate = false;
        int newSeverity = 0;
        double newClassScore = 0;
        Distress old = distress;

        for(Iterator i = store.getState().distressList().iterator(); i.hasNext();){

            Distress d = (Distress) i.next();

            Location.distanceBetween(
                    d.getGps().getLat(),
                    d.getGps().getLon(),
                    distress.getGps().getLat(),
                    distress.getGps().getLon(),
                    results);

            System.out.println("The distance b/w:" + results[0]);

            if(results[0] < Constants.NEW_DISTRESS_THRESHOLD){
                if (d.getDistress().equals(distress.getDistress())){
                    System.out.println("In distress check");
                    if(distress.getClassScore() > d.getClassScore()){
                        System.out.println("In distres class");
                        newClassScore = distress.getClassScore();
                        newSeverity = distress.getSeverity();
                        toUpdate = true;
                        old = d;
                        break;
                    }
                    isPresent = true;
                }
            }

        }

        if (socketConnection.getStatus()){
            if(!isPresent && !toUpdate){
                store.dispatch(new Action<>(StateAction.ADD_DISTRESS,distress));
                socketConnection.getSocket().emit("ADD_DISTRESS",gson.toJson(distress));
            }else if(isPresent && toUpdate){
                Distress newDistress = new Distress(old.getGps(),old.getDistress(),newSeverity,newClassScore,distress.getBoundingbox());
                updateDistress(newDistress);
            }
        }
    }

    public void setDistressList(LinkedList<Distress> distressList){
        store.dispatch(new Action<>(StateAction.ADD_DISTRESS_LIST,distressList));
    }

    public void updateDistress(Distress distress){
        store.dispatch(new Action<>(StateAction.UPDATE_DISTRESS,distress));
        socketConnection.getSocket().emit("UPDATE_DISTRESS",gson.toJson(distress));
    }

    public State getState(){
        return this.store.getState();
    }

    public SocketConnection getConnection(){
        return socketConnection;
    }

    class Logger<A, S> implements Store.Middleware<A, S> {
        private final String tag;
        public Logger(String tag) {
            this.tag = tag;
        }
        public void dispatch(Store<A, S> store, A action, Store.NextDispatcher<A> next) {
            Log.d(tag, "--> " + action.toString());
            next.dispatch(action);
            Log.d(tag, "<-- " + store.getState().toString());
        }
    }
}


