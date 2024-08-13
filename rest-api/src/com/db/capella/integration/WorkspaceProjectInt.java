// Copyright DB InfraGO AG and contributors
// SPDX-License-Identifier: Apache-2.0

package com.db.capella.integration;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import com.db.capella.model.WorkspaceProject;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class WorkspaceProjectInt extends WorkspaceProject {
  org.eclipse.core.resources.IProject obj;

  public WorkspaceProjectInt(org.eclipse.core.resources.IProject obj) {
    super();
    this.obj = obj;
    ReflectionUtils.copyProperties(obj, this);
    this.computeModified();
  }

  public void computeModified() {
    this.setModified(false);
    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
    for (IWorkbenchWindow window : windows) {
      IWorkbenchPage[] pages = window.getPages();
      for (IWorkbenchPage page : pages) {
        IEditorPart[] editors = page.getDirtyEditors();
        for (IEditorPart editor : editors) {
          if (editor.getEditorInput() instanceof FileEditorInput) {
            FileEditorInput input = (FileEditorInput) editor.getEditorInput();
            IResource resource = input.getFile();
            if (resource.getProject().equals(this.obj)) {
              // Found a dirty editor in the project
              this.setModified(true);
              return;
            }
          }
        }
      }
    }
  }

  public void save() {
    org.eclipse.core.resources.IProject obj = this.obj;
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow[] workbenchWindows = workbench.getWorkbenchWindows();

        for (IWorkbenchWindow window : workbenchWindows) {
          IWorkbenchPage[] pages = window.getPages();
          for (IWorkbenchPage page : pages) {
            IEditorPart[] editors = page.getDirtyEditors();
            for (IEditorPart editor : editors) {
              if (editor.getEditorInput() instanceof FileEditorInput) {
                FileEditorInput input = (FileEditorInput) editor.getEditorInput();
                IResource resource = input.getFile();
                org.eclipse.core.resources.IProject editorProject = resource.getProject();

                // Check if the editor is associated with the given
                // project
                if (obj.equals(editorProject)) {
                  // Save the editor
                  page.saveEditor(editor, false);
                }
              }
            }
          }
        }
      }
    });
  }

  public void setFullPath(IPath fullPath) {
    if (fullPath == null) {
      this.setFullPath("");
    } else {
      this.setFullPath(fullPath.toString());
    }
  }

  public void setLocation(IPath location) {
    if (location == null) {
      this.setLocation("");
    } else {
      this.setLocation(location.toString());
    }
  }
}
