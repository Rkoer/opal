/* BSD 2Clause License:
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
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED T
 * PURPOSE
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
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.PropertyKind

/**
 * Common supertrait of all factory method properties.
 */
sealed trait FactoryMethod extends Property {

    final def key: org.opalj.fpcf.PropertyKey = FactoryMethod.key

}

/**
 * Common constants use by all [[FactoryMethod]] properties associated with methods.
 */
object FactoryMethod extends PropertyMetaInformation {

    /**
     * The key associated with every FactoryMethod property.
     * It contains the unique name of the property and the default property that
     * will be used if no analysis is able to (directly) compute the respective property.
     * `IsFactoryMethod` is chosen as default because we have to define a sound default value for
     * all depended analyses.
     */
    final val key: org.opalj.fpcf.PropertyKey = PropertyKey.create("FactoryMethod", IsFactoryMethod)
}

/**
 * The respective method is a factory method.
 */
case object IsFactoryMethod extends FactoryMethod { final val isRefineable: Boolean = false }

/**
 * The respective method is not a factory method.
 */
case object NotFactoryMethod extends FactoryMethod { final val isRefineable: Boolean = false }

/**
 * This analysis identifies a project's factory methods.
 *
 * A method is no factory method if:
 *  - it is not static and does not invoke the constructor of the class where it is declared.
 *
 * This information is relevant in various contexts, e.g., to determine
 * the instantiability of a class.
 *
 * ==Usage==
 * Use the [[FPCFAnalysisManagerKey]] to query the analysis manager of a project. You can run
 * the analysis afterwards as follows:
 * {{{
 *  val analysisManager = project.get(FPCFAnalysisManagerKey)
 *  analysisManager.run(FactoryMethodAnalysis)
 * }}}
 * For detailed information see the documentation of the analysis manager.
 *
 * The results of this analysis are stored in the property store of the project. You can receive
 * the results as follows:
 * {{{
 * val theProjectStore = theProject.get(SourceElementsPropertyStoreKey)
 * val factoryMethods = theProjectStore.entities { (p: Property) ⇒
 * p == IsFactoryMethod
 * }
 * }}}
 *
 * @note Native methods are considered as factory methods because they might instantiate the class.
 */
class FactoryMethodAnalysis private (
        project: SomeProject
) extends AbstractFPCFAnalysis[Method](project, FactoryMethodAnalysis.entitySelector) {

    /**
     * Determines if the given method is a factory method.
     * Any
     * native method is considered as factory method, because we have to
     * assume, that it creates an instance of the class.
     * This checks not for effective factory methods, since the return type
     * of the method is ignored. A method is either native or the constructor
     * of the declaring class is invoked.
     *
     * Possible improvements:
     *  - check if the method returns an instance of the class or some superclass.
     */
    def determineProperty(method: Method): PropertyComputationResult = {

        //TODO use escape analysis (still have to be implemented).

        if (method.isNative)
            // We don't now what this static method is doing, hence, we assume that
            // it may act as a factory method; we can now abort the entire
            // analysis.
            return ImmediateResult(method, IsFactoryMethod)

        val classType = project.classFile(method).thisType

        val body = method.body.get
        val instructions = body.instructions
        val max = instructions.length
        var pc = 0
        while (pc < max) {
            val instruction = instructions(pc)
            if (instruction.opcode == INVOKESPECIAL.opcode) {
                instruction match {
                    case INVOKESPECIAL(`classType`, "<init>", _) ⇒
                        // We found a static factory method that is responsible
                        // for creating instances of this class.
                        return ImmediateResult(method, IsFactoryMethod)
                    case _ ⇒
                }
            }

            //TODO: model that the method could be called by an accessible method
            pc = body.pcOfNextInstruction(pc)
        }

        ImmediateResult(method, NotFactoryMethod)
    }
}

/**
 * Companion object for the [[FactoryMethodAnalysis]] class.
 */
object FactoryMethodAnalysis extends FPCFAnalysisRunner {

    /**
     * Selects all non-abstract static methods.
     */
    def entitySelector: PartialFunction[Entity, Method] = {
        case m: Method if m.isStatic && !m.isAbstract ⇒ m
    }

    def derivedProperties: Set[PropertyKind] = Set(FactoryMethod)

    protected[analysis] def start(project: SomeProject): Unit = {
        new FactoryMethodAnalysis(project)
    }
}
