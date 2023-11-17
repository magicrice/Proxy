package com.zky.client;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientProxy {
    //    Socket clientSocket;
    Socket cmdSocket;
    //    Socket intraSocket;
    Map<String, Socket> clientSocketMap = new ConcurrentHashMap<>();
    Map<String, Socket> intraSocketMap = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        new ClientProxy().run();

    }

    public void run() throws Exception {
        CmdClient cmdClient = new CmdClient();
        cmdClient.start();
        cmdClient.join();
    }

    public class Intra extends Thread {
        private String uuid;

        public Intra(String uuid) {
            this.uuid = uuid;
        }

        @Override
        public void run() {
            if (intraSocketMap.containsKey(uuid)) {
                try {
                    System.out.println("发送给服务端数据线程:"+Thread.currentThread().getName()+"-->"+uuid);
                    InputStream inputStream = intraSocketMap.get(uuid).getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    OutputStream outputStream = clientSocketMap.get(uuid).getOutputStream();
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
                    int i = 0;
                    while (true) {
                        int read = inputStreamReader.read();
                        if (read == -1) {
                            break;
                        }
                        System.out.print((char) read);
                        i++;
                        outputStreamWriter.write(read);
                        outputStreamWriter.flush();
                    }
                    System.out.println("发送报文数量:"+i);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class CmdClient extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    cmdSocket = new Socket("localhost", 9099);
                    InputStream inputStream = cmdSocket.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    String s = bufferedReader.readLine();
                    System.out.println(s);
                    if (s.startsWith("CREATE")) {
                        //创建终端通道并发送信息
                        s = s.replaceAll("CREATE-", "").replaceAll("\n", "");
                        System.out.println("创建通道");
                        new TerminalClient(s).start();
                        //请求创建通道
                        create(s);
                    }
                } catch (ConnectException e) {
                    e.printStackTrace();
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void create(String uuid) {
        try {
            Socket newSocket = new Socket("localhost", 8088);
            System.out.println("创建通道完成");
            System.out.println(uuid);
            clientSocketMap.put(uuid, newSocket);
            //向通道发送uuid
            OutputStream outputStream = newSocket.getOutputStream();
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
            bufferedWriter.write("CREATE-" + uuid);
            bufferedWriter.newLine();
            bufferedWriter.flush();
            System.out.println("发送创建成功信息");

        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    public class TerminalClient extends Thread {
        private String uuid;

        public TerminalClient(String uuid) {
            this.uuid = uuid;
        }

        @Override
        public void run() {
            while (true) {
                System.out.println(uuid);
                if (clientSocketMap.containsKey(uuid)) {
                    break;
                } else {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            InputStreamReader inputStreamReader = null;
            try {
                inputStreamReader = new InputStreamReader(clientSocketMap.get(uuid).getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            OutputStreamWriter outputStreamWriter = null;
            while (true) {
                try {
                    int read = inputStreamReader.read();
                    if (read == -1) {
                        break;
                    }
                    if (!intraSocketMap.containsKey(uuid)) {
                        Socket intraSocket = new Socket("localhost", 8080);
                        intraSocketMap.put(uuid, intraSocket);
                        OutputStream outputStream = intraSocket.getOutputStream();
                        outputStreamWriter = new OutputStreamWriter(outputStream);
                        new Intra(uuid).start();
                    }
                    outputStreamWriter.write(read);
                    outputStreamWriter.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

}
