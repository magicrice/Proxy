package com.zky.client;

import javax.swing.table.TableRowSorter;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientProxy {
    Map<String,Socket> cmdSocketMap = new ConcurrentHashMap<>();
    Map<String, Socket> clientSocketMap = new ConcurrentHashMap<>();
    Map<String, Socket> intraSocketMap = new ConcurrentHashMap<>();
    private static String serverIp = "localhost";
    private static List<String> list = new ArrayList<>();
    static {
        list.add("localhost:8080->7777");
        list.add("120.46.189.242:3306->9999");
    }
    private static Integer reConnectTime=5;

    public static void main(String[] args) throws Exception {
        new ClientProxy().run(list);
    }

    public void run(List<String> list) throws Exception {
        List<CmdClient> cmdClients = new ArrayList<>();
        for (String s : list) {
            String[] split = s.split("->");
            String[] split1 = split[0].split(":");
            CmdClient cmdClient = new CmdClient(split[1],split1[0],split1[1]);
            cmdClient.start();
            cmdClients.add(cmdClient);
            Thread.sleep(1000);
        }
        for (CmdClient cmdClient : cmdClients) {
            cmdClient.join();
        }
    }

    /**
     * 写给服务端
     */
    public class ClientToServer extends Thread {
        private String uuid;

        public ClientToServer(String uuid) {
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
//                        System.out.print((char) read);
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
        private String reflectPort;
        private String ip;
        private String port;
        public CmdClient(String reflectPort,String ip,String port){
            this.reflectPort = reflectPort;
            this.ip = ip;
            this.port = port;
        }
        @Override
        public void run() {
            System.out.println("线程idCmdClient-->"+Thread.currentThread().getName()+"->"+Thread.currentThread().getId());
            int num = 0;
            while (true) {
                try {
                    if(num > reConnectTime){
                        break;
                    }
                    if(!cmdSocketMap.containsKey(reflectPort) || cmdSocketMap.get(reflectPort).isClosed()){
                        Socket cmdSocket = new Socket(serverIp, 9099);
                        cmdSocketMap.put(reflectPort,cmdSocket);
                    }
                    OutputStream outputStream = cmdSocketMap.get(reflectPort).getOutputStream();
                    BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
                    bufferedWriter.write("PORT-"+reflectPort);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                    InputStream inputStream = cmdSocketMap.get(reflectPort).getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    String s = bufferedReader.readLine();
                    System.out.println(s);
                    if (s.startsWith("CREATE")) {
                        //创建终端通道并发送信息
                        s = s.replaceAll("CREATE-", "").replaceAll("\n", "").replaceAll("\r","");
                        System.out.println("创建通道");
                        new ClientToIntra(s,ip,port).start();
                        //请求创建通道
                        create(s);
                    }
                    if(s.startsWith("ERROR")){
                        //报错
                        s = s.replaceAll("ERROR-", "").replaceAll("\n", "").replaceAll("\r","");
                        System.out.println(s);
                    }
                    num = 0;
                } catch (Exception e) {
                    e.printStackTrace();
                    num++;
                    try {
                        if(cmdSocketMap.containsKey(port)){
                            cmdSocketMap.get(port).close();
                            cmdSocketMap.remove(port);
                        }
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }
                }
            }
        }
    }

    public void create(String uuid) {
        try {
            Socket newSocket = new Socket(serverIp, 8088);
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
    public class ClientToIntra extends Thread {
        private String uuid;
        private String ip;
        private String port;

        public ClientToIntra(String uuid,String ip,String port) {
            this.uuid = uuid;
            this.ip = ip;
            this.port = port;
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
                        Socket intraSocket = new Socket(ip, Integer.parseInt(port));
                        intraSocketMap.put(uuid, intraSocket);
                        System.out.println("目前存在代理服务通道数量："+intraSocketMap.size()+"->"+intraSocketMap.keySet());
                        outputStream = intraSocket.getOutputStream();
                        new ClientToServer(uuid).start();
                    }
                    int read = inputStream.read();
                    if (read == -1) {
                        System.out.println("服务端流关闭");
                        clientSocketMap.remove(uuid);
                        break;
                    }
                    if (!intraSocketMap.containsKey(uuid)) {
                        Socket intraSocket = new Socket(ip, Integer.parseInt(port));
                        intraSocketMap.put(uuid, intraSocket);
                        System.out.println("目前存在代理服务通道数量："+intraSocketMap.size()+"->"+intraSocketMap.keySet());
                        outputStream = intraSocket.getOutputStream();
                        new ClientToServer(uuid).start();
                    }

//                    System.out.print((char) read);
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
