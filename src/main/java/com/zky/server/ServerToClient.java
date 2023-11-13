package main.java.com.zky.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerToClient extends Thread{

    private Socket clientSocket;
    private OutputStream toOuter;
    private ServerProxy serverProxy;

    public ServerToClient(ServerProxy serverProxy){
        this.serverProxy = serverProxy;
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(8088);
            new ToOuter().start();
            while (true){
                 clientSocket = serverSocket.accept();
//                 Thread.sleep(10);
                 if(clientSocket != null){
                     break;
                 }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendToClient(InputStream is, OutputStream os){
        toOuter = os;

        while (true){
            try {
                int read = is.read();
                if(read == -1){
                    break;
                }
                System.out.print((char)read);
                clientSocket.getOutputStream().write(read);
                clientSocket.getOutputStream().flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class ToOuter extends Thread{

        @Override
        public void run() {
            try {
                while (true){
                    if(clientSocket != null){
                        break;
                    }
                }
                InputStream inputStream = clientSocket.getInputStream();
                while (true){
                    int read = inputStream.read();
                    System.out.print((char) read);
                    toOuter.write(read);
                    toOuter.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
