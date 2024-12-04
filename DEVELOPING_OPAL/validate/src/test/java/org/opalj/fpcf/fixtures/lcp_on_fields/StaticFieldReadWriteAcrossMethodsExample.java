/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.lcp_on_fields;

import org.opalj.fpcf.properties.lcp_on_fields.StaticValues;
import org.opalj.fpcf.properties.linear_constant_propagation.ConstantValue;
import org.opalj.fpcf.properties.linear_constant_propagation.ConstantValues;

public class StaticFieldReadWriteAcrossMethodsExample {
    private static int a;

    public void setATo11() {
        a = 11;
    }

    private void setATo42() {
        a = 42;
    }

    @StaticValues(constantValues = {
            @ConstantValue(variable = "a", value = 42)
    })
    @ConstantValues({
            @ConstantValue(variable = "lv3", value = 11),
            @ConstantValue(variable = "lv5", value = 42)
    })
    public static void main(String[] args) {
        StaticFieldReadWriteAcrossMethodsExample example = new StaticFieldReadWriteAcrossMethodsExample();

        example.setATo11();

        int a1 = a;

        example.setATo42();

        int a2 = a;

        System.out.println("a1: " + a1 + ", a2: " + a2);
    }
}
