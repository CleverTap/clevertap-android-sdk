package com.clevertap.android.directcall.utils;

import org.json.JSONObject;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketIOManager {
    private static Socket socket;
    private static Boolean isNewInstance = true;
    private static Boolean isUnAuthorized = false;

    private SocketIOManager() {
    }

    public static Socket getSocket(IO.Options options, String url) {
        if (socket == null) {
            try {
                socket = IO.socket(url, options);
                isNewInstance = true;
            } catch (Exception e) {
                //throw new RuntimeException(e);
            }
        }else {
            isNewInstance = false;
        }
        return socket;
    }

    public static void setSocket(Socket socket) {
        SocketIOManager.socket = socket;
    }

    public static Socket getSocket() {
        return socket;
    }

    public static Boolean getIsNewInstance() {
        return isNewInstance;
    }

    public static void closeSocketConnection(){
        if(socket!=null){
            socket.close();
            socket.off();
            socket = null;
        }
    }

    public static Boolean isSocketConnected(){
        if(socket!=null){
            return socket.connected();
        }else {
            return false;
        }
    }

    public static void resetSocketConfiguration(){
        if (isSocketConnected()) {
            if (getSocket() != null) {
                SocketIOManager.closeSocketConnection();
                SocketIOManager.setIsUnAuthorized(false);
            }
        }
    }

    public static void sendSuccessAck(Object... args) {
        JSONObject status = new JSONObject();
        try {
            status.put("status", true);
            Ack ack = (Ack) args[args.length - 1];
            ack.call(status);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static Boolean getIsUnAuthorized() {
        return isUnAuthorized;
    }

    public static void setIsUnAuthorized(Boolean isUnAuthorized) {
        SocketIOManager.isUnAuthorized = isUnAuthorized;
    }
}
