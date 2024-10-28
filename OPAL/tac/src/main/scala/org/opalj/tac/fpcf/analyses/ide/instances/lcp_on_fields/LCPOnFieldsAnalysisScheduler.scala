/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields

import scala.collection.immutable

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.PropertyBounds
import org.opalj.ide.integration.IDEPropertyMetaInformation
import org.opalj.ide.problem.IDEProblem
import org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.problem.LCPOnFieldsFact
import org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.problem.LCPOnFieldsProblem
import org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.problem.LCPOnFieldsValue
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.LinearConstantPropagationPropertyMetaInformation
import org.opalj.tac.fpcf.analyses.ide.integration.JavaIDEAnalysisScheduler
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement

/**
 * Linear constant propagation on fields as IDE analysis. This implementation is mainly intended to be an example of a
 * cyclic IDE analysis (see [[LCPOnFieldsProblem]]).
 */
abstract class LCPOnFieldsAnalysisScheduler extends JavaIDEAnalysisScheduler[LCPOnFieldsFact, LCPOnFieldsValue]
    with JavaIDEAnalysisScheduler.ForwardICFG {
    override def propertyMetaInformation: IDEPropertyMetaInformation[LCPOnFieldsFact, LCPOnFieldsValue] =
        LCPOnFieldsPropertyMetaInformation

    override def createProblem(project: SomeProject): IDEProblem[
        LCPOnFieldsFact,
        LCPOnFieldsValue,
        JavaStatement,
        Method
    ] = {
        new LCPOnFieldsProblem
    }

    override def uses: Set[PropertyBounds] =
        super.uses.union(immutable.Set(
            PropertyBounds.ub(LinearConstantPropagationPropertyMetaInformation)
        ))
}
