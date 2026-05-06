package com.auction.server;

import com.google.gson.Gson;

public class TestGson {
    public static void main(String[] args) {
        Gson gson = new Gson();

        String json = gson.toJson("hello");
        System.out.println(json);
    }
}
