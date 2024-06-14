/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.linear_constant_propagation;

import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.*;

/**
 * Annotation to state that a variable has a constant value
 */
@PropertyValidator(key = LinearConstantPropagationProperty.KEY, validator = ConstantValueMatcher.class)
@Repeatable(ConstantValues.class)
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface ConstantValue {
    /**
     * The name of the variable
     */
    String variable();

    /**
     * The constant value of the variable
     */
    int value();
}
