package com.zky.test;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

public class Test {
    public static void main(String[] args) {
        Set<String> strings = new CopyOnWriteArraySet<>();
        strings.add("1");
        strings.add("2");
        strings.add("3");
        strings.add("4");
        strings.add("5");
        Set<String> collect = strings.stream().skip(5).collect(Collectors.toSet());
        for (String s : collect) {
            System.out.println(s);
        }

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
