package tech.jvm;

public class Main {

    public static void main(String[] args) throws Exception {
        Hotspot hotspot = new Hotspot(
                "tech.code.Demo",
                "D:\\study\\code\\java\\write-my-own\\mini-jvm\\target\\classes");
        hotspot.start();
    }

}
