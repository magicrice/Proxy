package com.zky.test;

public class Test {
    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append("abc\n");
        System.out.println("\r");
        new Test().aa();

    }
    public void aa(){
        TT tt = new TT();
        tt.start();
        try {
            tt.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public class TT extends Thread{

        @Override
        public void run() {
            System.out.println(Thread.currentThread().getName()+"已运行");
        }

        @Override
        public void destroy() {
            System.out.println(Thread.currentThread().getName()+"已销毁");
        }
    }
}
