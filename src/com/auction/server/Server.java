package com.auction.server;

import com.google.gson.Gson;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private static final int PORT = 9000;
    private static final Auction auction = new Auction();
    private static final Set<PrintWriter> clients = ConcurrentHashMap.newKeySet();
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server started on port " + PORT + "...");

        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(() -> handleClient(socket)).start();
        }
    }

    private static void handleClient(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            clients.add(out);

            String line;
            while ((line = in.readLine()) != null) {
                try {
                    BidRequest req = gson.fromJson(line, BidRequest.class);

                    String result = auction.placeBid(req.bidder, req.amount);

                    broadcast(result);

                } catch (Exception e) {
                    out.println("[ERROR] Invalid JSON format");
                }
            }

        } catch (Exception e) {
            System.out.println("Client disconnected");
        }
    }

    private static void broadcast(String message) {
        for (PrintWriter client : clients) {
            client.println(message);
        }
    }
}