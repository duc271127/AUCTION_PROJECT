package com.auction.server;

import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;

public class Client {

    // DOMAIN + PORT PLAYIT
    private static final String HOST = "lungs-decree.with.playit.plus";
    private static final int PORT = 1125;

    public static void main(String[] args) {

        try {

            System.out.println("Connecting to server...");

            Socket socket = new Socket(HOST, PORT);

            System.out.println("Connected to auction server!");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(),
                    true
            );

            BufferedReader console = new BufferedReader(
                    new InputStreamReader(System.in)
            );

            Gson gson = new Gson();

            // Thread nhận dữ liệu từ server
            Thread receiveThread = new Thread(() -> {

                try {

                    String response;

                    while ((response = in.readLine()) != null) {
                        System.out.println(response);
                    }

                } catch (Exception e) {
                    System.out.println("Disconnected from server");
                }

            });

            receiveThread.start();

            System.out.println("Nhap bid theo format:");
            System.out.println("A 100");
            System.out.println("B 200");

            String input;

            while ((input = console.readLine()) != null) {

                try {

                    String[] parts = input.trim().split(" ");

                    if (parts.length != 2) {
                        System.out.println("[ERROR] Format dung: A 100");
                        continue;
                    }

                    String bidder = parts[0];

                    int amount = Integer.parseInt(parts[1]);

                    BidRequest req = new BidRequest();
                    req.bidder = bidder;
                    req.amount = amount;

                    String json = gson.toJson(req);

                    out.println(json);

                } catch (NumberFormatException e) {

                    System.out.println("[ERROR] Amount phai la so");

                } catch (Exception e) {

                    System.out.println("[ERROR] Format dung: A 100");

                }
            }

            socket.close();

        } catch (Exception e) {

            System.out.println("Khong connect duoc server");

            e.printStackTrace();
        }
    }
}
