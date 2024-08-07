/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.ide.lcp_on_fields

import org.opalj.fpcf.ide.IDEPropertiesTest
import org.opalj.fpcf.properties.lcp_on_fields.LCPOnFieldsProperty
import org.opalj.fpcf.properties.linear_constant_propagation.LinearConstantPropagationProperty

class LCPOnFieldsTests extends IDEPropertiesTest {
    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/lcp_on_fields")
    }

    describe("Execute the o.o.t.f.a.i.i.l.LCPOnFieldsAnalysis") {
        val testContext = executeAnalyses(Set(
            LinearConstantPropagationAnalysisSchedulerExtended,
            LCPOnFieldsAnalysisScheduler
        ))

        val entryPoints = methodsWithAnnotations(testContext.project)
        entryPoints.foreach { case (method, _, _) =>
            testContext.propertyStore.force(method, LCPOnFieldsAnalysisScheduler.property.key)
        }

        testContext.propertyStore.waitOnPhaseCompletion()
        testContext.propertyStore.shutdown()

        validateProperties(
            testContext,
            methodsWithAnnotations(testContext.project),
            Set(LCPOnFieldsProperty.KEY, LinearConstantPropagationProperty.KEY),
            failOnInterimResults = false
        )
    }
}
