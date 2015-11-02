/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package fpcf
package analysis

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.INVOKESPECIAL

/**
 * This analysis determines which classes can never be instantiated (e.g.,
 * `java.lang.Math`).
 *
 * A class is not instantiable if:
 *  - it only defines private constructors and these constructors are not called
 *    by any static method and the class does not implement Serializable.
 *
 * @note This analysis depends on the project configuration which encodes the analysis mode.
 *       Different analysis modes are: library with open or closed packages assumption or application
 *
 *  This information is relevant in various contexts, e.g., to determine
 * precise call graph. For example, instance methods of those objects that cannot be
 * created are always dead.
 *
 * ==Usage==
 * Use the [[FPCFAnalysisManagerKey]] to query the analysis manager of a project. You can run
 * the analysis afterwards as follows:
 * {{{
 *  val analysisManager = project.get(FPCFAnalysisManagerKey)
 *  analysisManager.run(InstantiabilityAnalysis)
 * }}}
 * For detailed information see the documentation of the analysis manager.
 *
 * The results of this analysis are stored in the property store of the project. You can receive
 * the results as follows:
 * {{{
 * val theProjectStore = theProject.get(SourceElementsPropertyStoreKey)
 * val instantiableClasses = theProjectStore.entities { (p: Property) ⇒
 * p == Instantiable
 * }
 * }}}
 *
 * This information is relevant in various contexts, e.g., to determine
 * precise call graph. For example, instance methods of those objects that cannot be
 * created are always dead.
 *
 * @note The analysis does not take reflective instantiations into account!
 */
class SimpleInstantiabilityAnalysis private (
    project: SomeProject
)
        extends AbstractLinearFPCFAnalysis[Method](
            project,
            SimpleInstantiabilityAnalysis.entitySelector
        ) {

    def determineProperty(
        method: Method
    ): Traversable[EP] = {
        var cfToProperty = Set.empty[EP]
        if (method.isNative) {
            //return all classes the package that matches the return type?
            println(method.descriptor.toJava(method.name))
        } else {

            //val returnType = method.returnType

            val body = method.body.get
            val instructions = body.instructions
            val max = instructions.length
            var pc = 0
            while (pc < max) {
                val instruction = instructions(pc)
                if (instruction.opcode == INVOKESPECIAL.opcode) {
                    instruction match {
                        case INVOKESPECIAL(classType, "<init>", _) ⇒ {
                            val classFile = project.classFile(classType)
                            if (classFile.nonEmpty)
                                cfToProperty += EP(classFile.get, Instantiable)
                        }
                        case _ ⇒
                    }
                }

                //TODO: model that the method could be called by an accessible method
                pc = body.pcOfNextInstruction(pc)
            }

        }
        cfToProperty
    }
}

/**
 * Companion object for the [[InstantiabilityAnalysis]] class.
 */
object SimpleInstantiabilityAnalysis
        extends FPCFAnalysisRunner[SimpleInstantiabilityAnalysis] {

    private[SimpleInstantiabilityAnalysis] def entitySelector: PartialFunction[Entity, Method] = {
        case m: Method if m.body.nonEmpty || m.isNative ⇒ m
    }

    private[SimpleInstantiabilityAnalysis] def apply(
        project: SomeProject
    ): SimpleInstantiabilityAnalysis = {
        new SimpleInstantiabilityAnalysis(project)
    }

    protected def start(project: SomeProject): Unit = {
        SimpleInstantiabilityAnalysis(project)
    }
}