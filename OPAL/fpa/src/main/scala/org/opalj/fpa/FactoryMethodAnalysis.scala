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
package br
package analyses
package fp

import org.opalj.fp.PropertyKey
import org.opalj.fp.Property
import org.opalj.fp.Entity
import org.opalj.br.Method
import org.opalj.fp.PropertyStore
import org.opalj.fp.PropertyComputationResult
import org.opalj.fp.Result
import org.opalj.fp.ImmediateResult
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.fp.Continuation
import org.opalj.fpa.FixpointAnalysis
import org.opalj.fpa.FilterEntities
import org.opalj.fpa.AssumptionDependentAnalysis
import java.net.URL

/**
 * Common supertrait of all factory method properties.
 */
sealed trait FactoryMethod extends Property {
    final def key = FactoryMethod.Key // All instances have to share the SAME key!
}

/**
 * Common constants use by all [[FactoryMethod]] properties associated with methods.
 */
object FactoryMethod {

    /**
     * The key associated with every purity property.
     * It contains the unique name of the property and the default property that
     * will be used if no analysis is able to (directly) compute the respective property.
     */
    final val Key = PropertyKey.create("FactoryMethod", IsFactoryMethod)
}

/**
 * The respective method is a factory method.
 */
case object IsFactoryMethod extends FactoryMethod { final val isRefineable = false }

/**
 * The respective method is not a factory method.
 */
case object NonFactoryMethod extends FactoryMethod { final val isRefineable = false }

object FactoryMethodAnalysis extends FixpointAnalysis
        with FilterEntities[Method]
        with AssumptionDependentAnalysis[Method] {

    private val opInvokeSpecial = INVOKESPECIAL.opcode

    val propertyKey = FactoryMethod.Key

    val entitySelector: PartialFunction[Entity, Method] = {
        case m: Method if m.isStatic && !m.isAbstract ⇒ m
    }

    //    private val opInvokeStatic = INVOKESTATIC.opcode

    private[this] def evaluateViaNativeContinuation(method: Method): Continuation = {
        (dependeeE: Entity, dependeeP: Property) ⇒
            if (dependeeP == Global)
                Result(method, IsFactoryMethod)
            else Result(method, NonFactoryMethod)
    }

    /**
     * Determines if the given method is used as effective factory method.
     */
    def determineProperty(
        method: Method)(
            implicit project: Project[URL],
            store: PropertyStore): PropertyComputationResult = {

        val classFile = project.classFile(method)
        val classType = classFile.thisType

        if (method.isNative) {
            import store.require
            return require(method, FactoryMethod.Key,
                method, ProjectAccessibility.Key)(evaluateViaNativeContinuation(method))
        }

        val body = method.body.get
        val instructions = body.instructions
        val max = instructions.length
        var pc = 0
        while (pc < max) {
            val instruction = instructions(pc)

            if (instruction.opcode == opInvokeSpecial) {
                val call = instruction.asInstanceOf[INVOKESPECIAL]
                if ((call.declaringClass eq classType) && call.name == "<init>") {
                    import store.require
                    return require(method, FactoryMethod.Key,
                        method, ProjectAccessibility.Key)(evaluateViaNativeContinuation(method))
                }
            }

            //TODO: model that the method could be called by an accessible method

            pc = instruction.indexOfNextInstruction(pc, body)
        }

        ImmediateResult(method, NonFactoryMethod)
    }

    //    def analyze(implicit project: Project[URL]): Unit = {
    //        implicit val projectStore = project.get(SourceElementsPropertyStoreKey)
    //        val filter: PartialFunction[Entity, Method] = {
    //            case m: Method if m.isStatic && !m.isAbstract ⇒ m
    //        }
    //
    //        val analysisMode = AnalysisModes.withName(project.config.as[String]("org.opalj.analysisMode"))
    //
    //        projectStore <||< (filter, determineUsageAsFactory(analysisMode))
    //    }
}