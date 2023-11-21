package com.zky.client;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientProxy {
    Socket cmdSocket;
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

    /**
     * 写给服务端
     */
    public class Intra extends Thread {
        private String uuid;

        public Intra(String uuid) {
            this.uuid = uuid;
        }

        @Override
        public void run() {
            System.out.println("线程idIntra-->"+Thread.currentThread().getName()+"->"+Thread.currentThread().getId());
            if (intraSocketMap.containsKey(uuid)) {
                try {
                    System.out.println("发送给服务端数据线程:"+Thread.currentThread().getName()+"-->"+uuid);
                    InputStream inputStream = intraSocketMap.get(uuid).getInputStream();
                    OutputStream outputStream = clientSocketMap.get(uuid).getOutputStream();
                    while (true) {
                        int read = inputStream.read();
                        if (read == -1) {
                            System.out.println("被代理服务流关闭");
                            intraSocketMap.remove(uuid);
                            break;
                        }
                        outputStream.write(read);
                        outputStream.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 连接cmd服务端
     */
    public class CmdClient extends Thread {
        @Override
        public void run() {
            System.out.println("线程idCmdClient-->"+Thread.currentThread().getName()+"->"+Thread.currentThread().getId());
            while (true) {
                try {
                    if(cmdSocket == null || cmdSocket.isClosed()){
                        cmdSocket = new Socket("localhost", 9099);
                    }
                    InputStream inputStream = cmdSocket.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    String s = bufferedReader.readLine();
                    System.out.println(s);
                    if (s.startsWith("CREATE")) {
                        //创建终端通道并发送信息
                        s = s.replaceAll("CREATE-", "").replaceAll("\n", "").replaceAll("\r","");
                        System.out.println("创建通道");
                        new TerminalClient(s).start();
                        //请求创建通道
                        create(s);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
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
            System.out.println("目前存在客户端通道数量："+clientSocketMap.size()+"->"+clientSocketMap.keySet());
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

    /**
     * 请求被代理服务
     */
    public class TerminalClient extends Thread {
        private String uuid;

        public TerminalClient(String uuid) {
            this.uuid = uuid;
        }

        @Override
        public void run() {
            System.out.println("线程idTerminalClient-->"+Thread.currentThread().getName()+"->"+Thread.currentThread().getId());
            while (true) {
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
            System.out.println("开始请求被代理服务器");
            InputStream inputStream = null;
            try {
                inputStream = clientSocketMap.get(uuid).getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            OutputStream outputStream = null;
            while (true) {
                try {
                if (!intraSocketMap.containsKey(uuid)) {
//                    Socket intraSocket = new Socket("212.129.183.69", 3306); //8.0
                    Socket intraSocket = new Socket("120.46.189.242", 3306);//5.7
//                    Socket intraSocket = new Socket("localhost", 22);
//                    Socket intraSocket = new Socket("localhost", 8080);
                    intraSocketMap.put(uuid, intraSocket);
                    System.out.println("目前存在代理服务通道数量："+intraSocketMap.size()+"->"+intraSocketMap.keySet());
                    outputStream = intraSocket.getOutputStream();
                    new Intra(uuid).start();
                }
                    int read = inputStream.read();
                    if (read == -1) {
                        System.out.println("服务端流关闭");
                        clientSocketMap.remove(uuid);
                        break;
                    }
                    outputStream.write(read);
                    outputStream.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

    }

}
