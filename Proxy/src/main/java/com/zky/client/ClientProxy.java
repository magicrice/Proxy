package com.zky.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class ClientProxy {

    private Socket outerServerSocket;

    private Socket intraServerSocket;

    public static void main(String[] args) throws IOException {
        new ClientProxy().start();
    }

    public void start() throws IOException {

        // 连接到服务器
        outerServerSocket = new Socket("localhost", 8888);
        System.out.println("Connected to outerServer");

        InputStream outerNetInputStream = outerServerSocket.getInputStream();
        BufferedReader outerNetInputStreamReader = new BufferedReader(new InputStreamReader(outerNetInputStream));
        BufferedWriter outputStreamWriter = null;
        while (true) {

            try {

                int read = outerNetInputStreamReader.read();
                if (-1 == read) {
                    break ;
                }

                if (intraServerSocket == null || intraServerSocket.isClosed()) {
                    intraServerSocket = new Socket("localhost", 8080);
                    System.out.println("Connected to intraServer");
                    outputStreamWriter = new BufferedWriter(new OutputStreamWriter(intraServerSocket.getOutputStream()));
                    new IntraToOutThread().start();
                }

                System.out.print((char) read);
                outputStreamWriter.write(read);
                outputStreamWriter.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    class IntraToOutThread extends Thread {

        @Override
        public void run() {

            try {

                InputStream inputStream = intraServerSocket.getInputStream();

                OutputStream outputStream = outerServerSocket.getOutputStream();

                while (true) {

                    int read = inputStream.read();
                    if (-1 == read) {
                        break;
                    }

                    System.out.print((char) read);
                    outputStream.write(read);
                    outputStream.flush();

                }

                System.out.println();
                System.out.println();
                System.out.println("intraNet传输完毕");
                System.out.println();
                System.out.println();
                intraServerSocket.close();

            } catch (Exception e) {
                try {
                    intraServerSocket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                e.printStackTrace();
            }

        }

    }

}
