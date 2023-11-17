package com.zky.test;

public class Test {
    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append("abc\n");
        System.out.println(sb.toString().endsWith("\n"));
        System.out.println(sb.toString().substring(0,sb.length()-1));
    }
}
