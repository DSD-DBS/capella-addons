package org.db.capellacollabmanager;

import java.time.LocalTime;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.impl.BasicEObjectImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.sirius.business.api.session.Session;
import org.eclipse.sirius.business.api.session.SessionManager;
import org.eclipse.sirius.viewpoint.DRepresentationDescriptor;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.polarsys.capella.common.data.modellingcore.ModelElement;
import org.polarsys.capella.common.re.RecCatalog;
import org.polarsys.capella.core.data.capellamodeller.SystemEngineering;
import org.polarsys.kitalpha.vp.requirements.Requirements.Module;
import org.polarsys.kitalpha.vp.requirements.Requirements.Requirement;

import com.google.gson.Gson;

public class SelectedElementsBroadCastService implements IStartup {
    final Gson gson = new Gson();
    @Override
    public void earlyStartup() {
        System.out.println("Selected elements broadcast service has been started.");
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        LocalTime currentTime = LocalTime.now();
                        System.out.println("---------- " + currentTime + " ---------------");
                        try {
                            IWorkbench workbench = PlatformUI.getWorkbench();
                            if (workbench == null)
                                return;
                            IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
                            if ((window == null)
                                    || !(window.getSelectionService().getSelection() instanceof TreeSelection))
                                return;
                            TreeSelection selection = (TreeSelection) window.getSelectionService().getSelection();
                            if (selection == null)
                                return;
                            Object[] objs = selection.toArray();
                            if (objs == null)
                                return;
                            String msg = "", label = "", id = "", modelName = "", modelId = "", modelPath = "";
                            for (Object obj : objs) {
                                IProject project = null;
                                if (objs.length == 1 ) {
                                    if (obj instanceof BasicEObjectImpl) {
                                        BasicEObjectImpl element = (BasicEObjectImpl) obj;
                                        Resource resource = element.eResource();
                                        URI uri = resource.getURI();
                                        if (uri.isPlatformResource()) {
                                            String platformString = uri.toPlatformString(true);
                                            IResource workspaceResource =
                                            ResourcesPlugin.getWorkspace().getRoot().findMember(platformString);
                                            if (workspaceResource != null) {
                                                project = workspaceResource.getProject();
                                                modelName = project.getName();
                                            }
                                        }
                                    }
                                    if (project != null) {
                                        Collection<Session> sessions = SessionManager.INSTANCE.getSessions();
                                        for (Session session : sessions) {
                                            URI sessionResourceURI = session.getSessionResource().getURI();
                                            IFile sessionFile = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(sessionResourceURI.toPlatformString(true)));
                                            if (sessionFile != null && sessionFile.getProject().equals(project)) {
                                                modelPath = sessionFile.getFullPath().toPortableString();
                                            }
                                        }
                                    }
                                    EObject currentElement = (EObject)obj;
                                    while (currentElement != null && !(currentElement instanceof SystemEngineering)) {
                                        currentElement = currentElement.eContainer();
                                    }
                                    if (currentElement instanceof SystemEngineering) {
                                        SystemEngineering systemEngineering = (SystemEngineering) currentElement;
                                        modelId = systemEngineering.getId();
                                    }
                                }
                                if (obj instanceof ModelElement) {
                                    ModelElement element = (ModelElement) obj;
                                    label = element.getLabel();
                                    id = element.getId();
                                    msg += "\n" + id + ": (" + obj.getClass().getSimpleName() + ") \"" + label + "\"";
                                } else if (obj instanceof Module) {
                                    Module element = (Module) obj;
                                    label = element.getReqIFName();
                                    id = element.getId();
                                    msg += "\n" + id + ": (" + obj.getClass().getSimpleName() + ") \"" + label + "\"";
                                } else if (obj instanceof RecCatalog) {
                                    RecCatalog element = (RecCatalog) obj;
                                    label = element.getName();
                                    id = element.getId();
                                    msg += "\n" + id + ": (" + obj.getClass().getSimpleName() + ") \"" + label + "\"";
                                } else if (obj instanceof Requirement) {
                                    Requirement element = (Requirement) obj;
                                    label = element.getReqIFName();
                                    id = element.getId();
                                    msg += "\n" + id + ": (" + obj.getClass().getSimpleName() + ") \"" + label + "\"";
                                } else if (obj instanceof DRepresentationDescriptor) {
                                    DRepresentationDescriptor element = (DRepresentationDescriptor) obj;
                                    label = element.getName();
                                    id = element.getUid();
                                    msg += "\n" + id + ": (" + obj.getClass().getSimpleName() + ") \"" + label + "\"";
                                }
                                msg += ", model_name: \"" + modelName + "\"";
                                msg += ", model_path: " + modelPath + "";
                                msg += ", model_uuid: " + modelId + "";
                                System.out.println(msg);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        };
        timer.scheduleAtFixedRate(task, 0, 1000);
    }
}
