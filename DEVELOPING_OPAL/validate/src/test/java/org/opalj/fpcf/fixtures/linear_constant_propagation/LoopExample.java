/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.linear_constant_propagation;

import org.opalj.fpcf.properties.linear_constant_propagation.ConstantValue;
import org.opalj.fpcf.properties.linear_constant_propagation.VariableValue;

public class LoopExample {
    public static int loop1(int a) {
        int res = 0;

        while (a > 0) {
            a--;
            res++;
        }

        return res;
    }

    public static int loop2(int a) {
        int res = a - 1;

        while (a > 0) {
            a--;
            res += 2;
            System.out.println(res);
            res -= 2;
        }

        return res;
    }

    @ConstantValue(variable = "lv3", value = 22)
    @VariableValue(variable = "lv1")
    public static void main(String[] args) {
        int i = loop1(42);
        int j = loop2(23);

        System.out.println("i: " + i + ", j: " + j);
    }
}
