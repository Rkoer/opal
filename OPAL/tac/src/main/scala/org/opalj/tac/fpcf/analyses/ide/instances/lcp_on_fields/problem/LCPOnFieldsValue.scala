/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.problem

import scala.collection.immutable

import org.opalj.ide.problem.IDEValue
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationValue

/**
 * Type for modeling values for linear constant propagation on fields
 */
trait LCPOnFieldsValue extends IDEValue

/**
 * Value not known (yet)
 */
case object UnknownValue extends LCPOnFieldsValue

/**
 * Value representing the state of an object
 */
case class ObjectValue(values: immutable.Map[String, LinearConstantPropagationValue]) extends LCPOnFieldsValue {
    override def toString: String = s"ObjectValue(${values.mkString(", ")})"
}

/**
 * Value representing the state of an array
 */
case class ArrayValue(
    initValue: LinearConstantPropagationValue,
    elements:  immutable.Map[Int, LinearConstantPropagationValue]
) extends LCPOnFieldsValue {
    override def toString: String = s"ArrayValue($initValue, ${elements.mkString(", ")})"
}

/**
 * Value is variable (not really used currently, mainly for completeness)
 */
case object VariableValue extends LCPOnFieldsValue
