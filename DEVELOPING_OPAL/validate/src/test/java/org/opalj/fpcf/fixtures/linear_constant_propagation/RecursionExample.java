package org.opalj.fpcf.fixtures.linear_constant_propagation;

import org.opalj.fpcf.properties.linear_constant_propagation.ConstantValue;

public class RecursionExample {
    public static int recursive1(int a) {
        if (a > 0) {
            a -= 2;
            System.out.println(recursive1(a));
            a += 2;
        }

        return a + 3;
    }

    @ConstantValue(variable = "lv1", value = 14)
    public static void main(String[] args) {
        int i = recursive1(11);

        System.out.println("i: " + i);
    }
}
