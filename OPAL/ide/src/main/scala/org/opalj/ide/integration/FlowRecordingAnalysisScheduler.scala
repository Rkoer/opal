/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ide.integration

import scala.annotation.tailrec

import java.io.File
import java.io.FileNotFoundException
import java.io.FileWriter
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import scala.collection.mutable

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.ide.problem.FlowRecorderModes
import org.opalj.ide.problem.FlowRecorderModes.FlowRecorderMode
import org.opalj.ide.problem.FlowRecordingIDEProblem
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEProblem
import org.opalj.ide.problem.IDEValue
import org.opalj.ide.solver.ICFG
import org.opalj.ide.solver.IDEAnalysis
import org.opalj.ide.util.Logging

/**
 * Wrapper class for a normal IDE analysis scheduler for debugging purposes. Records the flow paths the IDE solver takes
 * for a given base problem as graph and writes it to a file in DOT format.
 * DOT files can either be viewed with a suitable local program or online e.g. at
 * [[https://dreampuf.github.io/GraphvizOnline]].
 * @param path the location to write the resulting DOT file (either a file ending with `.dot` or a directory)
 * @param uniqueFlowsOnly whether to drop or to keep duplicated flows
 * @param recordEdgeFunctions whether to record edge functions too or just stick with the flow
 */
class FlowRecordingAnalysisScheduler[Fact <: IDEFact, Value <: IDEValue, Statement, Callable <: Entity](
    ideAnalysisScheduler: IDEAnalysisScheduler[Fact, Value, Statement, Callable],
    path:                 Option[Path]     = Some(Paths.get("target/flow-recordings")),
    recorderMode:         FlowRecorderMode = FlowRecorderModes.NODE_AS_STMT_AND_FACT,
    uniqueFlowsOnly:      Boolean          = true,
    recordEdgeFunctions:  Boolean          = true
) extends IDEAnalysisScheduler[Fact, Value, Statement, Callable] with Logging.EnableAll with Logging.GlobalLogContext {
    override def propertyMetaInformation: IDEPropertyMetaInformation[Fact, Value] = {
        ideAnalysisScheduler.propertyMetaInformation
    }

    override def createProblem(project: SomeProject): IDEProblem[Fact, Value, Statement, Callable] = {
        val flowRecordingProblem = new FlowRecordingIDEProblem(
            ideAnalysisScheduler.createProblem(project),
            createICFG(project),
            recorderMode,
            uniqueFlowsOnly,
            recordEdgeFunctions
        )
        startRecording(project, flowRecordingProblem)
        flowRecordingProblem
    }

    private var icfg: ICFG[Statement, Callable] = _

    override def createICFG(project: SomeProject): ICFG[Statement, Callable] = {
        if (icfg == null) {
            icfg = ideAnalysisScheduler.createICFG(project)
        }
        icfg
    }

    override def requiredProjectInformation: ProjectInformationKeys = {
        ideAnalysisScheduler.requiredProjectInformation
    }

    override def uses: Set[PropertyBounds] = {
        ideAnalysisScheduler.uses
    }

    override def beforeSchedule(project: SomeProject, propertyStore: PropertyStore): Unit = {
        ideAnalysisScheduler.beforeSchedule(project, propertyStore)
    }

    override def afterPhaseScheduling(propertyStore: PropertyStore, analysis: FPCFAnalysis): Unit = {
        ideAnalysisScheduler.afterPhaseScheduling(propertyStore, analysis)
    }

    override def afterPhaseCompletion(
        project:       SomeProject,
        propertyStore: PropertyStore,
        analysis:      FPCFAnalysis
    ): Unit = {
        ideAnalysisScheduler.afterPhaseCompletion(project, propertyStore, analysis)
        stopRecording(
            analysis.asInstanceOf[IDEAnalysis[Fact, Value, Statement, Callable]]
                .problem.asInstanceOf[FlowRecordingIDEProblem[Fact, Value, Statement, Callable]]
        )
    }

    /**
     * Associate used writers with the file they write to
     */
    private val fileByWriter = mutable.Map.empty[Writer, File]

    /**
     * Get the file to write the graph to. If [[path]] references a file then this method will return [[path]]. If
     * [[path]] references a directory, a new filename is created (s.t. no file gets overwritten).
     */
    private def getFile(project: SomeProject): File = {
        lazy val className = {
            val classFQN = project.projectClassFilesWithSources.head._1.thisType.fqn
            classFQN.substring(classFQN.lastIndexOf('/') + 1)
        }

        @tailrec
        def getNextFreeWithBasePath(basePath: Path, index: Int = 1): Path = {
            val pathToCheck =
                if (index == 0) {
                    basePath.resolve(s"$className-flow-recording.dot")
                } else {
                    basePath.resolve(s"$className-flow-recording-$index.dot")
                }

            if (Files.exists(pathToCheck)) {
                getNextFreeWithBasePath(basePath, index + 1)
            } else {
                pathToCheck
            }
        }

        val completePath = path match {
            case Some(p) =>
                if (p.toUri.getPath.endsWith(".dot")) {
                    p
                } else {
                    getNextFreeWithBasePath(p)
                }
            case None => getNextFreeWithBasePath(Paths.get("."))
        }

        completePath.toFile
    }

    private def startRecording(
        project:              SomeProject,
        flowRecordingProblem: FlowRecordingIDEProblem[Fact, Value, Statement, Callable]
    ): Unit = {
        logDebug("starting recording")

        val file = getFile(project)
        val directoryAsFile = file.getParentFile
        val directoryAsPath = directoryAsFile.toPath.toAbsolutePath.normalize()
        if (!directoryAsFile.exists()) {
            if (directoryAsPath.startsWith(Paths.get(".").toAbsolutePath.normalize())) {
                logWarn(s"creating directory '$directoryAsPath' as it didn't exist!")
                directoryAsFile.mkdirs()
            } else {
                throw new FileNotFoundException(
                    s"Directory '$directoryAsPath' does not exist! Directories outside of the project directory are not created automatically!"
                )
            }
        }

        val writer = new FileWriter(file)
        fileByWriter.put(writer, file)

        flowRecordingProblem.startRecording(writer)
    }

    private def stopRecording(flowRecordingProblem: FlowRecordingIDEProblem[Fact, Value, Statement, Callable]): Unit = {
        logDebug("stopping recording")

        val writer = flowRecordingProblem.stopRecording()

        writer.close()

        logInfo(s"wrote flow recording to '${fileByWriter(writer)}'")
    }
}
