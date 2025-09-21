package tech.code;

public class Demo {

    public static void main(String[] args) {
        System.out.println(1);
        System.out.println(max(1, 2));
    }

    public static int max(int a, int b) {
        if (a < b)
            return b;
        return a;
    }

}
