package com.zky.test;

import java.io.*;
import java.net.Socket;

public class Client {
    public static void main(String[] args) throws IOException {
            Socket localhost = new Socket("localhost", 9999);
        OutputStream outputStream = localhost.getOutputStream();
        for (int i = 0; i < 1000; i++) {
            outputStream.write(i);
            outputStream.flush();
        }
    }
}
