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

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.editor.event.{DocumentEvent, DocumentListener}
import com.intellij.openapi.fileEditor._
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.ToolWindowImpl
import com.intellij.openapi.wm.{ToolWindowAnchor, ToolWindowManager}
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.dom.{AST, ASTParser}

object JdtAstViewProjectComponent {
  final val key: String = "org.santoslab.jdt.astview.enabled"
  final val enabledText: String = "Disable JDT AST View"
  final val enabledDescription: String = "Disable JDT AST view tracking of Java source code"
  final val disabledText: String = "Enable JDT AST View"
  final val disabledDescription: String = "Enable JDT AST view tracking of Java source code"
  var isSwitchActionEnabled: Boolean = {
    val pc = PropertiesComponent.getInstance
    pc.getBoolean(key, false)
  }

  def performSwitchAction(project: Project, file: VirtualFile): Unit = {
    isSwitchActionEnabled = !isSwitchActionEnabled
    val pc = PropertiesComponent.getInstance
    pc.setValue(key, isSwitchActionEnabled)
    if (isSwitchActionEnabled) JdtAstViewProjectComponent.resetView(project)
    else {
      JdtAstViewProjectComponent.toolWindowFactory(project, { f =>
        val tw = f.toolWindow.asInstanceOf[ToolWindowImpl]
        tw.activate(() => {
          JdtAstViewProjectComponent.updateAstView(project, file)
        })
      })
    }
  }

  def resetView(project: Project): Unit = {
    toolWindowFactory(project, { f =>
      f.astView.treeModel.setRoot(JdtAstViewToolWindowFactory.emptyRoot)
    })
  }

  def updateAstView(project: Project, file: VirtualFile): Unit =
    if (isSwitchActionEnabled) resetView(project)
    else {
      val editor = FileEditorManager.
        getInstance(project).getSelectedTextEditor
      if (editor == null) return
      val document = editor.getDocument
      val source = document.getText.toCharArray
      val parser = ASTParser.newParser(AST.JLS8)
      parser.setSource(source)
      val options = JavaCore.getOptions
      JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options)
      parser.setCompilerOptions(options)
      val cu = parser.createAST(null)
      toolWindowFactory(project, { f =>
        val tree = f.astView.tree
        for (sl <- tree.getTreeSelectionListeners) {
          tree.removeTreeSelectionListener(sl)
        }
        f.astView.treeModel.setRoot(TreeNodeAdapter("root", 0, Some(Left(cu))))
        tree.addTreeSelectionListener({ e =>
          if (!editor.isDisposed) {
            Option(tree.getLastSelectedPathComponent.asInstanceOf[TreeNodeAdapter]).foreach { tna =>
              FileEditorManager.getInstance(project).openTextEditor(
                new OpenFileDescriptor(project, file, tna.offset), true)
            }
          }
        })
      })
    }

  def toolWindowFactory(project: Project,
                        g: JdtAstViewToolWindowFactory.Forms => Unit): Unit =
    Option(JdtAstViewToolWindowFactory.windows.get(project)).foreach(g)
}

import org.santoslab.intellij.jdt.astview.JdtAstViewProjectComponent._

class JdtAstViewProjectComponent(project: Project) extends ProjectComponent {
  override def projectClosed(): Unit = {}

  override def projectOpened(): Unit = {
    val tw = ToolWindowManager.getInstance(project).
      registerToolWindow("JDT AST", false, ToolWindowAnchor.RIGHT)
    JdtAstViewToolWindowFactory.createToolWindowContent(project, tw)

    project.getMessageBus.connect(project).
      subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER,
        new FileEditorManagerListener {
          override def fileClosed(source: FileEditorManager,
                                  file: VirtualFile): Unit = {
            resetView(project)
          }

          override def fileOpened(source: FileEditorManager,
                                  file: VirtualFile): Unit = {
            if (file.getExtension != "java") resetView(project)
            else {
              source.getSelectedTextEditor.getDocument.addDocumentListener(
                new DocumentListener {
                  override def documentChanged(event: DocumentEvent): Unit =
                    updateAstView(project, file)

                  override def beforeDocumentChange(event: DocumentEvent): Unit = {}
                })
              updateAstView(project, file)
            }
          }

          override def selectionChanged(event: FileEditorManagerEvent): Unit =
            if (event.getNewFile != null)
              if (event.getNewFile.getExtension != "java")
                resetView(project)
              else
                updateAstView(project, event.getNewFile)
        })
  }

  override def initComponent(): Unit = {}

  override def disposeComponent(): Unit = {}

  override def getComponentName: String = "JDT AstView Project"
}
