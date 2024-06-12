/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.solver

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.ide.solver.ICFG
import org.opalj.tac.fpcf.analyses.ifds.JavaForwardICFG

/**
 * Interprocedural control flow graph for Java programs
 */
class JavaICFG(project: SomeProject) extends ICFG[JavaStatement, Method] {
    // TODO (IDE) CURRENTLY DEPENDS ON IMPLEMENTATION FROM IFDS
    private val baseICFG = new JavaForwardICFG(project)

    override def getStartStatements(callable: Method): Set[JavaStatement] =
        baseICFG.startStatements(callable).map {
            case org.opalj.tac.fpcf.analyses.ifds.JavaStatement(method, index, code, cfg) =>
                JavaStatement(method, index, isReturnNode = false, code, cfg)
        }

    override def getNextStatements(stmt: JavaStatement): Set[JavaStatement] = {
        if (isCallStatement(stmt)) {
            Set(JavaStatement(stmt.method, stmt.index, isReturnNode = true, stmt.code, stmt.cfg))
        } else {
            baseICFG.nextStatements(
                org.opalj.tac.fpcf.analyses.ifds.JavaStatement(stmt.method, stmt.index, stmt.code, stmt.cfg)
            ).map {
                case org.opalj.tac.fpcf.analyses.ifds.JavaStatement(method, index, code, cfg) =>
                    JavaStatement(method, index, isReturnNode = false, code, cfg)
            }
        }
    }

    override def isNormalExitStatement(stmt: JavaStatement): Boolean = {
        stmt.index == stmt.basicBlock.asBasicBlock.endPC &&
        stmt.basicBlock.successors.exists(_.isNormalReturnExitNode)
    }

    override def isAbnormalExitStatement(stmt: JavaStatement): Boolean = {
        stmt.index == stmt.basicBlock.asBasicBlock.endPC &&
        stmt.basicBlock.successors.exists(_.isAbnormalReturnExitNode)
    }

    override def isCallStatement(stmt: JavaStatement): Boolean = {
        getCalleesIfCallStatement(stmt).nonEmpty
    }

    // TODO (IDE) REFACTOR AS 'getCallees(...): Set[Method]'
    override def getCalleesIfCallStatement(stmt: JavaStatement): Option[Set[Method]] = {
        if (stmt.isReturnNode) {
            None
        } else {
            baseICFG.getCalleesIfCallStatement(
                org.opalj.tac.fpcf.analyses.ifds.JavaStatement(stmt.method, stmt.index, stmt.code, stmt.cfg)
            )
        }
    }

    override def getCallable(stmt: JavaStatement): Method = stmt.method

    def getCallablesCallableFromOutside: Set[Method] = {
        baseICFG.methodsCallableFromOutside.map { declaredMethod => declaredMethod.asDefinedMethod.definedMethod }
    }

    override def stringifyStatement(stmt: JavaStatement, indent: String = "", short: Boolean = false): String = {
        val stringifiedStatement = stmt.toString
        if (short) {
            stringifiedStatement.substring(0, stringifiedStatement.indexOf("{"))
        } else {
            stringifiedStatement
        }
    }
}
