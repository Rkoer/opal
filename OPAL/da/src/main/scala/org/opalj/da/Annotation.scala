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
package da

import scala.xml.Node
import scala.xml.Text
import scala.xml.NodeSeq

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 */
case class Annotation(
        type_index:          Constant_Pool_Index,
        element_value_pairs: IndexedSeq[ElementValuePair] = IndexedSeq.empty
) {

    final def attribute_length: Int = {
        2 + element_value_pairs.foldLeft(2 /*num_...*/ )((c, n) ⇒ c + n.attribute_length)
    }

    def toXHTML(implicit cp: Constant_Pool): Node = {
        val annotationType = parseFieldType(cp(type_index).toString)

        val evps: NodeSeq =
            if (element_value_pairs.nonEmpty) {
                val evpsAsXHTML = this.element_value_pairs.map(_.toXHTML)
                Seq(
                    Text("("),
                    <ol class="element_value_pairs">{ evpsAsXHTML }</ol>,
                    Text(")")
                )
            } else {
                NodeSeq.Empty
            }

        <div class="annotation">
            { annotationType.asSpan("annotation_type") }
            { evps }
        </div>
    }
}
