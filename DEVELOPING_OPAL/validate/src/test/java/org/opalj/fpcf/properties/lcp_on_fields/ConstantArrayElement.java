/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.lcp_on_fields;

import java.lang.annotation.*;

/**
 * Annotation to state that an array element has a constant value
 */
@Documented
@Target(ElementType.ANNOTATION_TYPE)
public @interface ConstantArrayElement {
    /**
     * The index of the element
     */
    int index();

    /**
     * The constant value of the element
     */
    int value();
}
