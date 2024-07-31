/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ide.problem

import java.io.Writer
import scala.collection.mutable

import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyStore

object FlowRecorderModes extends Enumeration {
    type FlowRecorderMode = Value

    /**
     * A node in the graph is only made up of a statement. The edges are annotated with the propagated facts.
     */
    val NODE_AS_STMT: FlowRecorderModes.Value = Value
    /**
     * A node in the graph is the combination of a statement and a fact.
     */
    val NODE_AS_STMT_AND_FACT: FlowRecorderModes.Value = Value
}

/**
 * Wrapper class for a normal IDE problem for debugging purposes. Records the flow paths the IDE solver takes for a
 * given base problem as graph and writes it to a file in DOT format.
 * DOT files can either be viewed with a suitable local program or online e.g. at
 * [[https://dreampuf.github.io/GraphvizOnline]].
 * @param baseProblem the base problem that defines the flows and edge functions that should be analyzed
 * @param uniqueFlowsOnly whether to drop or to keep duplicated flows
 * @param recordEdgeFunctions whether to record edge functions too or just stick with the flow
 */
class FlowRecordingIDEProblem[Fact <: IDEFact, Value <: IDEValue, Statement, Callable <: Entity](
        val baseProblem:         IDEProblem[Fact, Value, Statement, Callable],
        val recorderMode:        FlowRecorderModes.FlowRecorderMode = FlowRecorderModes.NODE_AS_STMT,
        val uniqueFlowsOnly:     Boolean                            = true,
        val recordEdgeFunctions: Boolean                            = false
) extends IDEProblem[Fact, Value, Statement, Callable](baseProblem.icfg) {
    /**
     * Wrapper class for flow functions doing the actual recording
     */
    private class RecordingFlowFunction(
            baseFlowFunction: FlowFunction[Fact],
            val source:       Statement,
            val target:       Statement,
            val flowType:     String
    ) extends FlowFunction[Fact] {
        override def compute(sourceFact: Fact): collection.Set[Fact] = {
            val facts = baseFlowFunction.compute(sourceFact)
            facts.foreach { fact => collectedFlows.addOne(createDotEdge(source, sourceFact, target, fact, flowType)) }
            facts
        }
    }

    private type DotEdge = (Statement, Fact, Statement, Fact, String)

    private val collectedFlows = mutable.ListBuffer.empty[DotEdge]

    private val collectedEdgeFunctions = mutable.Map.empty[DotEdge, EdgeFunction[Value]]

    private var writer: Writer = null

    override val nullFact: Fact = baseProblem.nullFact

    override val lattice: MeetLattice[Value] = baseProblem.lattice

    override def getNormalFlowFunction(source: Statement, target: Statement)(
        implicit propertyStore: PropertyStore
    ): FlowFunction[Fact] = {
        new RecordingFlowFunction(baseProblem.getNormalFlowFunction(source, target), source, target, "normal flow")
    }

    override def getCallFlowFunction(
        callSite:    Statement,
        calleeEntry: Statement,
        callee:      Callable
    )(implicit propertyStore: PropertyStore): FlowFunction[Fact] = {
        new RecordingFlowFunction(
            baseProblem.getCallFlowFunction(callSite, calleeEntry, callee),
            callSite,
            calleeEntry,
            "call flow"
        )
    }

    override def getReturnFlowFunction(
        calleeExit: Statement,
        callee:     Callable,
        returnSite: Statement
    )(implicit propertyStore: PropertyStore): FlowFunction[Fact] = {
        new RecordingFlowFunction(
            baseProblem.getReturnFlowFunction(calleeExit, callee, returnSite),
            calleeExit,
            returnSite,
            "return flow"
        )
    }

    override def getCallToReturnFlowFunction(callSite: Statement, callee: Callable, returnSite: Statement)(
        implicit propertyStore: PropertyStore
    ): FlowFunction[Fact] = {
        new RecordingFlowFunction(
            baseProblem.getCallToReturnFlowFunction(callSite, callee, returnSite),
            callSite,
            returnSite,
            "call-to-return flow"
        )
    }

    override def getNormalEdgeFunction(
        source:     Statement,
        sourceFact: Fact,
        target:     Statement,
        targetFact: Fact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[Value] = {
        val edgeFunctionResult = baseProblem.getNormalEdgeFunction(source, sourceFact, target, targetFact)
        collectedEdgeFunctions.put(
            createDotEdge(source, sourceFact, target, targetFact, "normal flow"),
            getEdgeFunctionFromEdgeFunctionResult(edgeFunctionResult)
        )
        edgeFunctionResult
    }

    override def getCallEdgeFunction(
        callSite:        Statement,
        callSiteFact:    Fact,
        calleeEntry:     Statement,
        calleeEntryFact: Fact,
        callee:          Callable
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[Value] = {
        val edgeFunctionResult =
            baseProblem.getCallEdgeFunction(callSite, callSiteFact, calleeEntry, calleeEntryFact, callee)
        collectedEdgeFunctions.put(
            createDotEdge(callSite, callSiteFact, calleeEntry, calleeEntryFact, "call flow"),
            getEdgeFunctionFromEdgeFunctionResult(edgeFunctionResult)
        )
        edgeFunctionResult
    }

    override def getReturnEdgeFunction(
        calleeExit:     Statement,
        calleeExitFact: Fact,
        callee:         Callable,
        returnSite:     Statement,
        returnSiteFact: Fact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[Value] = {
        val edgeFunctionResult =
            baseProblem.getReturnEdgeFunction(calleeExit, calleeExitFact, callee, returnSite, returnSiteFact)
        collectedEdgeFunctions.put(
            createDotEdge(calleeExit, calleeExitFact, returnSite, returnSiteFact, "return flow"),
            getEdgeFunctionFromEdgeFunctionResult(edgeFunctionResult)
        )
        edgeFunctionResult
    }

    override def getCallToReturnEdgeFunction(
        callSite:       Statement,
        callSiteFact:   Fact,
        returnSite:     Statement,
        returnSiteFact: Fact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[Value] = {
        val edgeFunctionResult =
            baseProblem.getCallToReturnEdgeFunction(callSite, callSiteFact, returnSite, returnSiteFact)
        collectedEdgeFunctions.put(
            createDotEdge(callSite, callSiteFact, returnSite, returnSiteFact, "call-to-return flow"),
            getEdgeFunctionFromEdgeFunctionResult(edgeFunctionResult)
        )
        edgeFunctionResult
    }

    private def createDotEdge(
        source:     Statement,
        sourceFact: Fact,
        target:     Statement,
        targetFact: Fact,
        flowType:   String
    ): DotEdge = {
        (source, sourceFact, target, targetFact, flowType)
    }

    private def getEdgeFunctionFromEdgeFunctionResult(
        edgeFunctionResult: EdgeFunctionResult[Value]
    ): EdgeFunction[Value] = {
        edgeFunctionResult match {
            case FinalEdgeFunction(edgeFunction)             => edgeFunction
            case InterimEdgeFunction(interimEdgeFunction, _) => interimEdgeFunction
        }
    }

    /**
     * Start recording
     * @param writer to write the graph to
     */
    def startRecording(writer: Writer): Unit = {
        this.writer = writer
        collectedFlows.clear()
        collectedEdgeFunctions.clear()

        writer.write("digraph G {\n\tnodesep=\"2.0\";\n\tranksep=\"1.5\";\n")
    }

    private def stringifyDotEdge(dotEdge: DotEdge): String = {
        val (source, sourceFact, target, targetFact, flowType) = dotEdge

        var fromNode: String = null
        var toNode: String = null
        var label: String = null
        recorderMode match {
            case FlowRecorderModes.NODE_AS_STMT =>
                fromNode = s"${icfg.stringifyStatement(source, short = true)}"
                toNode = s"${icfg.stringifyStatement(target, short = true)}"
                if (recordEdgeFunctions) {
                    label = s"$targetFact\\n($flowType),\\n${collectedEdgeFunctions(dotEdge)}"
                } else {
                    label = s"$targetFact\\n($flowType)"
                }
            case FlowRecorderModes.NODE_AS_STMT_AND_FACT =>
                fromNode = s"(${icfg.stringifyStatement(source, short = true)}, $sourceFact)"
                toNode = s"(${icfg.stringifyStatement(target, short = true)}, $targetFact)"
                if (recordEdgeFunctions) {
                    label = s"$flowType,\\n${collectedEdgeFunctions(dotEdge)}"
                } else {
                    label = flowType
                }
        }

        s"\t\"$fromNode\" -> \"$toNode\" [label=\"$label\"]\n"
    }

    /**
     * Stop recording and finish writing
     */
    def stopRecording(): Writer = {
        if (uniqueFlowsOnly) {
            val seenFlows = mutable.Set.empty[String]
            collectedFlows.foreach { dotEdge =>
                val stringDotEdge = stringifyDotEdge(dotEdge)
                if (!seenFlows.contains(stringDotEdge)) {
                    seenFlows.add(stringDotEdge)
                    writer.write(stringDotEdge)
                }
            }
        } else {
            collectedFlows.foreach { dotEdge => writer.write(stringifyDotEdge(dotEdge)) }
        }

        writer.write("}\n")
        writer.flush()

        val writerTmp = writer
        this.writer = null

        collectedFlows.clear()
        collectedEdgeFunctions.clear()

        writerTmp
    }
}
