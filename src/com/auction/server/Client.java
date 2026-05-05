package com.auction.server;

import java.io.*;
import java.net.*;
import com.google.gson.Gson;

public class Client {

    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("localhost", 9000);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(
                socket.getOutputStream(), true);
        BufferedReader console = new BufferedReader(
                new InputStreamReader(System.in));

        Gson gson = new Gson();

        // Thread nhận dữ liệu realtime
        new Thread(() -> {
            try {
                String serverMsg;
                while ((serverMsg = in.readLine()) != null) {

                    try {
                        Message m = gson.fromJson(serverMsg, Message.class);

                        if (m.type.equals("NEW_BID")) {
                            System.out.println(
                                    "[" + m.type + "] "
                                            + m.user + " -> " + m.amount
                                            + " | " + m.content
                            );
                        }

                    } catch (Exception e) {
                        System.out.println(serverMsg);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // Gửi bid
        String msg;
        while ((msg = console.readLine()) != null) {
            out.println(msg);
        }
    }
}
