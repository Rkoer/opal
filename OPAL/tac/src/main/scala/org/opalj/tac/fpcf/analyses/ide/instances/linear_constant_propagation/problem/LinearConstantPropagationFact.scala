/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem

import org.opalj.ide.problem.IDEFact

/**
 * Type for modeling facts for linear constant propagation
 */
trait LinearConstantPropagationFact extends IDEFact

/**
 * Fact to use as null fact
 */
case object NullFact extends LinearConstantPropagationFact

/**
 * Fact representing a seen variable
 * @param name the name of the variable (e.g. `lv0`)
 * @param definedAtIndex where the variable is defined (used to uniquely identify a variable/variable fact)
 */
case class VariableFact(name: String, definedAtIndex: Int) extends LinearConstantPropagationFact {
    override def toString: String = s"VariableFact($name)"
}
