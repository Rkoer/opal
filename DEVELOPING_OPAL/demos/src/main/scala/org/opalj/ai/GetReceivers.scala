/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ai

import java.net.URL

import scala.collection.mutable

import org.opalj.br.PCAndInstruction
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.NEW
import org.opalj.br.Method
import org.opalj.ai.domain.Origin

/**
 * Extracts the information about receivers of method calls.
 *
 * @author Michael Eichberg
 */
object GetReceivers extends DefaultOneStepAnalysis {

    override def title: String = "Method Call Receivers information"

    override def description: String = "Provides information about a method call's receiver."

    override def doAnalyze(p: Project[URL], params: Seq[String], isInterrupted: () ⇒ Boolean): BasicReport = {

        p.updateProjectInformationKeyInitializationData(
            org.opalj.ai.common.SimpleAIKey,
            (_: Option[_]) ⇒ { (m: Method) ⇒
                // new org.opalj.ai.domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse(p, m)
                new org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse(p, m)
            }
        )
        org.opalj.ai.common.SimpleAIKey

        val ai = p.get(org.opalj.ai.common.SimpleAIKey)

        val counts: mutable.Map[String, Int] = mutable.Map.empty.withDefaultValue(0)

        p.parForeachMethodWithBody() { mi ⇒
            val m = mi.method
            for {
                PCAndInstruction(pc, i) ← m.body.get
                if i.isMethodInvocationInstruction
                invocationInstruction = i.asMethodInvocationInstruction
                if invocationInstruction.isVirtualMethodCall
                if invocationInstruction.methodDescriptor.returnType.isObjectType
                aiResult = ai(m).asInstanceOf[AIResult { val domain: Domain with Origin }]
                receiverPosition = invocationInstruction.methodDescriptor.parametersCount
                operands = aiResult.operandsArray(pc)
                if operands != null
            } {
                val receiver = operands(receiverPosition)
                // TODO Add special support for union types.
                val value = receiver.asDomainReferenceValue
                val utb = value.upperTypeBound
                var s =
                    if (utb.isSingletonSet)
                        utb.head.toJava
                    else
                        utb.map(_.toJava).mkString("⋂(", " with ", ")")

                s += ", "+value.isPrecise
                val triviallyPrecise =
                    value.isPrecise &&
                        value.upperTypeBound.isSingletonSet &&
                        !value.upperTypeBound.head.isArrayType && (
                            p.classFile(value.upperTypeBound.head.asObjectType).get.isFinal ||
                            p.classHierarchy.hasSubtypes(value.upperTypeBound.head.asObjectType).isNoOrUnknown
                        )
                if (triviallyPrecise)
                    s += " (trivially)"

                s += ", "+value.isNull
                if (aiResult.domain.origins(value.asInstanceOf[aiResult.domain.DomainValue]).forall(pc ⇒
                    pc >= 0 && {
                        val i = m.body.get.instructions(pc)
                        i.opcode == NEW.opcode || i.isLoadConstantInstruction
                    }))
                    s += " (trivially)"

                this.synchronized {
                    val newCount = counts(s) + 1
                    counts(s) = newCount
                }
            }
        }

        BasicReport(
            "type, isPrecise, isNull, count\n"+
                counts
                .iterator
                .map(e ⇒ e._1+", "+e._2)
                .mkString("\n")
        )
    }
}
