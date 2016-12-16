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

import java.util.concurrent.ConcurrentHashMap
import javax.swing.tree.{DefaultTreeCellRenderer, DefaultTreeModel, TreeSelectionModel}

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.ContentFactory

object JdtAstViewToolWindowFactory {
  final case class Forms(toolWindow: ToolWindow, astView: AstViewForm)

  final val emptyRoot = TreeNodeAdapter("root", 0, None)

  final val windows = new ConcurrentHashMap[Project, Forms]()

  def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = {
    toolWindow.setAutoHide(false)
    val contentFactory = ContentFactory.SERVICE.getInstance
    val astView = new AstViewForm()
    astView.treeModel = new DefaultTreeModel(emptyRoot)
    val cr = astView.tree.getCellRenderer.asInstanceOf[DefaultTreeCellRenderer]
    cr.setClosedIcon(null)
    cr.setOpenIcon(null)
    cr.setLeafIcon(null)
    astView.tree.getSelectionModel.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION)
    astView.tree.setModel(astView.treeModel)
    val content = contentFactory.createContent(astView.panel, "JDT AST", false)
    toolWindow.getContentManager.addContent(content)
    windows.put(project, Forms(toolWindow, astView))
  }

  def removeToolWindow(project: Project): Unit = {
    windows.remove(project)
  }
}
