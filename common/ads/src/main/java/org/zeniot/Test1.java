package org.zeniot;

import java.util.Arrays;

/**
 * @author Jack Wu
 */
public class Test1 {
    public static void main(String[] args) {
        byte[] a = new byte[4];
        int length = 851;
        a[3] = (byte) ((length >> 24) & 0xFF);
        a[2] = (byte) ((length >> 16) & 0xFF);
        a[1] = (byte) ((length >> 8) & 0xFF);
        a[0] = (byte) (length & 0xFF);
        System.out.println(Arrays.toString(a));
    }
}
