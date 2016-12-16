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
import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent, CommonDataKeys}
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.ToolWindowImpl

class SwitchViewAction extends AnAction {
  final val key: String = "org.santoslab.jdt.astview.enabled"
  final val enabledText: String = "Disable JDT AST View"
  final val enabledDescription: String = "Disable JDT AST view tracking of Java source code"
  final val disabledText: String = "Enable JDT AST View"
  final val disabledDescription: String = "Enable JDT AST view tracking of Java source code"

  override def actionPerformed(e: AnActionEvent): Unit = {
    val pc = PropertiesComponent.getInstance
    val isEnabled = pc.getBoolean(key, false)
    pc.setValue(key, !isEnabled)
    if (isEnabled) JdtAstViewProjectComponent.resetView(e.getProject)
    else {
      val file = e.getData[VirtualFile](CommonDataKeys.VIRTUAL_FILE)
      JdtAstViewProjectComponent.toolWindowFactory(e.getProject, { f =>
        val tw = f.toolWindow.asInstanceOf[ToolWindowImpl]
        tw.activate(() => {
          JdtAstViewProjectComponent.updateAstView(e.getProject, file)
        })
      })
    }
  }

  override def update(e: AnActionEvent): Unit = {
    val pc = PropertiesComponent.getInstance
    val isEnabled = pc.getBoolean(key, false)
    val p = e.getPresentation
    if (isEnabled) {
      p.setText(enabledText)
      p.setDescription(enabledDescription)
    } else {
      p.setText(disabledText)
      p.setDescription(disabledDescription)
    }
  }
}
