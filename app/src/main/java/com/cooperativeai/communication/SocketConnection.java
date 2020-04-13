/*
 * Created by Sujoy Datta. Copyright (c) 2020. All rights reserved.
 *
 * To the person who is reading this..
 * When you finally understand how this works, please do explain it to me too at sujoydatta26@gmail.com
 * P.S.: In case you are planning to use this without mentioning me, you will be met with mean judgemental looks and sarcastic comments.
 */

package com.cooperativeai.communication;

import android.app.Application;


import com.cooperativeai.statemanagement.MainStore;
import com.cooperativeai.statemanagement.StateProps.Distress;
import com.cooperativeai.utils.Constants;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.LinkedList;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SocketConnection extends Application {

    private Socket mSocket;
    private MainStore mainStore;
    private String token;
    private Gson gson = new Gson();
    private boolean isReady = false;
    private boolean auth = false;

    {
        try{
            mSocket = IO.socket(Constants.SOCKET_SERVER_URL);
        }catch(URISyntaxException e){
            throw new RuntimeException(e);
        }
    }

    public SocketConnection(MainStore mainStore, String token){
        this.mainStore = mainStore;
        this.token = token;
        mSocket.connect();
        initListeners();
    }

    public Socket getSocket(){
        return mSocket;
    }

    public boolean getStatus(){return isReady;}

    public boolean getAuthStatus(){ return auth; }

    public void initListeners(){
        mSocket.on("connected",onConnected);
        mSocket.on("AUTH_SUCCESS",onAuthSuccess);
        mSocket.on("AUTH_FAILED",onAuthFailed);
        mSocket.on("ADD_DISTRESS",onADD_DISTRESS);
        mSocket.on(Socket.EVENT_CONNECT, onConnect);
        mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR,onConnectError);
        mSocket.on("RESPONCE_STATE",onResposeState);
        mSocket.on("UPDATE_DISTRESS",onUpdate_DISTRESS);
        mSocket.on("MAP_RESPONSE",onMapResponse);
    }

    public void desposeListeners(){
        mSocket.off(Socket.EVENT_CONNECT, onConnect);
        mSocket.off("AUTH_SUCCESS",onAuthSuccess);
        mSocket.off("AUTH_FAILED",onAuthFailed);
        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.off("connected",onConnected);
        mSocket.off("ADD_DISTRESS",onADD_DISTRESS);
        mSocket.off(Socket.EVENT_CONNECT_ERROR,onConnectError);
        mSocket.off("RESPONCE_STATE",onResposeState);
        mSocket.off("UPDATE_DISTRESS",onUpdate_DISTRESS);
        mSocket.off("MAP_RESPONSE",onMapResponse);
        mSocket.disconnect();
    }

    //Listeners
    private Emitter.Listener onConnected = args -> new Thread(() -> {
        System.out.println("Connected to new Server");
//        System.out.println("Distress:" + gson.toJson(mainStore.getState().gps()));
        mSocket.emit("AUTHENTICATE", token);
    }).start();

    private Emitter.Listener onAuthSuccess = (args) -> {
        new Thread(() -> {
            this.auth = true;
            System.out.println("Authentication Successful");
            mSocket.emit("init",gson.toJson(mainStore.getState().gps()));
        }).start();
    };

    private Emitter.Listener onAuthFailed = (args) -> {
        new Thread(() -> {
            System.out.println("Authentication Failed");
            try {
                Thread.sleep(2000);
            }catch (Exception e){
                System.out.println(e);
            }
            mSocket.connect();
        }).start();
    };

    private Emitter.Listener onADD_DISTRESS = args -> new Thread(() -> {
        Distress distress = gson.fromJson((String) args[0],Distress.class);
        mainStore.addDistress(distress);
    }).start();

    private Emitter.Listener onUpdate_DISTRESS = args -> new Thread(() -> {
        Distress distress = gson.fromJson((String) args[0], Distress.class);
        mainStore.updateDistress(distress);
    }).start();

    private Emitter.Listener onConnect = (args) -> {
        new Thread(() -> {
            System.out.println("Connected to server");
        }).start();
    };


    private Emitter.Listener onMapResponse = (args -> {
        new Thread(() -> {
            LinkedList<Distress> distressList = getList((String) args[0],Distress.class);
            System.out.println(args[0]);
        }).start();
    });

    private Emitter.Listener onDisconnect = (args) -> {
        new Thread(()->{
            System.out.println("Disconnected to server");
        }).start();
    };

    private Emitter.Listener onConnectError = (args) -> {
        new Thread(() -> {
            System.out.println("Connection Error");
        }).start();
    };

    private Emitter.Listener onResposeState = (args) -> {
        new Thread(() -> {
            LinkedList<Distress> distressList = getList((String) args[0],Distress.class);
            mainStore.setDistressList(distressList);
            isReady = true;
        }).start();
    };

    public <Distress> LinkedList<Distress> getList(String jsonArray, Class<Distress> clazz) {
        Type typeOfT = TypeToken.getParameterized(LinkedList.class, clazz).getType();
        return new Gson().fromJson(jsonArray, typeOfT);
    }
}
