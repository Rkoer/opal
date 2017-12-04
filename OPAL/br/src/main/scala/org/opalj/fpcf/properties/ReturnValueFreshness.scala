/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package properties

sealed trait ReturnValueFreshnessPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = ReturnValueFreshness
}

/**
 * The property describes, whether a method returns a value, that is allocated in the same method
 * and only has escape state [[EscapeViaReturn]].
 */
sealed abstract class ReturnValueFreshness extends Property
        with ReturnValueFreshnessPropertyMetaInformation {

    final def key: PropertyKey[ReturnValueFreshness] = ReturnValueFreshness.key
}

object ReturnValueFreshness extends ReturnValueFreshnessPropertyMetaInformation {
    final lazy val key: PropertyKey[ReturnValueFreshness] = PropertyKey.create(
        // Name of the property
        "ReturnValueFreshness",
        // fallback value
        NoFreshReturnValue,
        // cycle-resolution strategy
        NoFreshReturnValue
    )
}

case object FreshReturnValue extends ReturnValueFreshness {
    override def isRefineable = false
}

case object NoFreshReturnValue extends ReturnValueFreshness {
    override def isRefineable = true
}

case object ConditionalFreshReturnValue extends ReturnValueFreshness {
    override def isRefineable = true
}

