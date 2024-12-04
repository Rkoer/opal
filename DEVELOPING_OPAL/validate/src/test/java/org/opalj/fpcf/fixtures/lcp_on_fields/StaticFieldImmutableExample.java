/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.lcp_on_fields;

import org.opalj.fpcf.properties.lcp_on_fields.StaticValues;
import org.opalj.fpcf.properties.linear_constant_propagation.ConstantValue;
import org.opalj.fpcf.properties.linear_constant_propagation.ConstantValues;
import org.opalj.fpcf.properties.linear_constant_propagation.VariableValue;

public final class StaticFieldImmutableExample {
    protected static int a = 42;
    static int b;
    private static int c;
    private static int d;

    static {
        b = 23;

        if (System.out != null) {
            c = 11;
        } else {
            c = 12;
        }
    }

    @StaticValues(constantValues = {
            @ConstantValue(variable = "a", value = 42),
            @ConstantValue(variable = "b", value = 23),
            @ConstantValue(variable = "d", value = 0)
    }, variableValues = {
            @VariableValue(variable = "c")
    })
    @ConstantValues({
            @ConstantValue(variable = "lv0", value = 42),
            @ConstantValue(variable = "lv1", value = 23),
            @ConstantValue(variable = "lv3", value = 0)
    })
    @VariableValue(variable = "lv2")
    public static void main(String[] args) {
        int a = StaticFieldImmutableExample.a;
        int b = StaticFieldImmutableExample.b;
        int c = StaticFieldImmutableExample.c;
        int d = StaticFieldImmutableExample.d;

        System.out.println("a: " + a + ", b: " + b + ", c: " + c + ", d: " + d);
    }
}
