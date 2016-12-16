/*
 Copyright (c) 2016, Robby, Kansas State University
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, this
    list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.santoslab.intellij.jdt.astview

import java.util
import javax.swing.tree.TreeNode
import org.eclipse.jdt.core.dom._

final case class TreeNodeAdapter(id: String, offset: Int,
                                 nodeOrChildren: Option[Either[ASTNode, java.util.Vector[TreeNodeAdapter]]]) extends TreeNode {
  lazy val _children: java.util.Vector[TreeNodeAdapter] = {
    import scala.collection.JavaConverters._
    var result = new java.util.Vector[TreeNodeAdapter]
    nodeOrChildren match {
      case Some(Left(node)) =>
        val l = node.structuralPropertiesForType
        if (l != null)
          for (sp <- l.asScala) {
            sp match {
              case sp: ChildPropertyDescriptor =>
                if (node.getStructuralProperty(sp) != null) {
                  val cn = node.getStructuralProperty(sp).asInstanceOf[ASTNode]
                  result.add(TreeNodeAdapter(s"${sp.getId} = ${cn.getClass}",
                    cn.getStartPosition, Some(Left(cn))))
                }
              case sp: SimplePropertyDescriptor =>
                if (node.getStructuralProperty(sp) != null) {
                  val v = node.getStructuralProperty(sp)
                  result.add(TreeNodeAdapter(s"${sp.getId} = $v", node.getStartPosition, None))
                }
              case sp: ChildListPropertyDescriptor =>
                if (node.getStructuralProperty(sp) != null) {
                  val result2 = new java.util.Vector[TreeNodeAdapter]()
                  val cns = node.getStructuralProperty(sp).asInstanceOf[java.util.List[ASTNode]]
                  for (cn <- cns.asScala) {
                    result2.add(TreeNodeAdapter(cn.getClass.toString,
                      cn.getStartPosition, Some(Left(cn))))
                  }
                  result.add(TreeNodeAdapter(sp.getId.toUpperCase,
                    node.getStartPosition, Some(Right(result2))))
                }
            }
          }
      case Some(Right(r)) => result = r
      case _ =>
    }
    result
  }

  override def children(): util.Enumeration[_] = _children.elements

  override def isLeaf: Boolean = _children.isEmpty

  override def getIndex(node: TreeNode): Int = _children.indexOf(node)

  override def getParent: TreeNode = null

  override def getChildCount: Int = _children.size

  override def getAllowsChildren: Boolean = !_children.isEmpty

  override def getChildAt(childIndex: Int): TreeNode = _children.get(childIndex)

  override def toString: String = id
}
