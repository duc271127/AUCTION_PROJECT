package com.auction.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import com.google.gson.Gson;

public class Server {

    static Auction auction = new Auction();
    static ExecutorService pool = Executors.newFixedThreadPool(10);
    static List<PrintWriter> clients = new CopyOnWriteArrayList<>();
    static Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        ServerSocket server = new ServerSocket(9000);
        System.out.println("Server started on port 9000...");

        while (true) {
            Socket socket = server.accept();
            pool.execute(() -> handleClient(socket));
        }
    }

    static void handleClient(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(), true);

            clients.add(out);

            String msg;

            while ((msg = in.readLine()) != null) {
                try {
                    String[] parts = msg.trim().split(" ");

                    //  thiếu dữ liệu
                    if (parts.length < 2) {
                        out.println("[ERROR] Format: <name> <amount>");
                        continue;
                    }

                    String user = parts[0];

                    int amount;
                    try {
                        amount = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        out.println("[ERROR] Amount must be number");
                        continue;
                    }

                    boolean success = auction.placeBid(user, amount);

                    if (success) {
                        Message m = new Message(
                                "NEW_BID",
                                user,
                                amount,
                                "New highest bid"
                        );
                        broadcast(gson.toJson(m));
                    } else {
                        out.println("[FAIL] Bid too low");
                    }

                } catch (Exception e) {
                    out.println("[ERROR] Invalid input");
                }
            }

        } catch (Exception e) {
            System.out.println("Client disconnected");
        }
    }

    static void broadcast(String message) {
        for (PrintWriter client : clients) {
            client.println(message);
        }
    }
}