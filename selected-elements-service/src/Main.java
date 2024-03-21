import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main implements IStartup {
    public class SelectedElement {
        @JsonProperty("element_uuid")
        private String elementUuid;

        @JsonProperty("element_name")
        private String elementName;

        @JsonProperty("model_uuid")
        private String modelUuid;

        @JsonProperty("model_path")
        private String modelPath;

        public SelectedElement() {
        }

        public SelectedElement(String elementUuid, String elementName, String modelUuid,
                String modelPath) {
            this.elementUuid = elementUuid;
            this.elementName = elementName;
            this.modelUuid = modelUuid;
            this.modelPath = modelPath;
        }

        public String getElementUuid() {
            return elementUuid;
        }

        public void setElementUuid(String elementUuid) {
            this.elementUuid = elementUuid;
        }

        public String getElementName() {
            return elementName;
        }

        public void setElementName(String elementName) {
            this.elementName = elementName;
        }

        public String getModelUuid() {
            return modelUuid;
        }

        public void setModelUuid(String modelUuid) {
            this.modelUuid = modelUuid;
        }

        public String getModelPath() {
            return modelPath;
        }

        public void setModelPath(String modelPath) {
            this.modelPath = modelPath;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            SelectedElement that = (SelectedElement) o;
            return Objects.equals(elementUuid, that.elementUuid) &&
                    Objects.equals(elementName, that.elementName) &&
                    Objects.equals(modelUuid, that.modelUuid) &&
                    Objects.equals(modelPath, that.modelPath);
        }

        @Override
        public int hashCode() {
            return Objects.hash(elementUuid, elementName, modelUuid, modelPath);
        }
    }

    public class SelectedElementList {
        private List<SelectedElement> elements = new ArrayList<>();;

        public SelectedElementList() {
        }

        public SelectedElementList(List<SelectedElement> elements) {
            this.elements = elements;
        }

        public List<SelectedElement> getElements() {
            return elements;
        }

        public void setElements(List<SelectedElement> elements) {
            this.elements = elements;
        }

        public void addElement(SelectedElement element) {
            this.elements.add(element);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            SelectedElementList that = (SelectedElementList) o;
            return Objects.equals(elements, that.elements);
        }

        @Override
        public int hashCode() {
            return Objects.hash(elements);
        }
    }

    public static void sendPostRequest(String targetUrl, String jsonInputString) {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(targetUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.out.println("POST request failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    public void earlyStartup() {
        String selectedElementsServiceTargetUrl;
        String capellaCollabSessionId;
        try {
            capellaCollabSessionId = System.getenv("CAPELLACOLLAB_SESSION_ID");
            if (capellaCollabSessionId == null) {
                throw new IllegalStateException("CAPELLACOLLAB_SESSION_ID environment variable is not set.");
            }
            selectedElementsServiceTargetUrl = System.getenv("SELECTED_ELEMENTS_SERVICE_TARGET_URL");
            if (selectedElementsServiceTargetUrl == null) {
                throw new IllegalStateException(
                        "SELECTED_ELEMENTS_SERVICE_TARGET_URL environment variable is not set.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        SelectedElementList prevSelectedElementList = new SelectedElementList();
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        SelectedElementList selectedElementList = new SelectedElementList();
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
                            String label = "", id = "", modelId = "", modelPath = "";
                            for (Object obj : objs) {
                                IProject project = null;
                                if (obj instanceof BasicEObjectImpl) {
                                    BasicEObjectImpl element = (BasicEObjectImpl) obj;
                                    Resource resource = element.eResource();
                                    URI uri = resource.getURI();
                                    if (uri.isPlatformResource()) {
                                        String platformString = uri.toPlatformString(true);
                                        IResource workspaceResource = ResourcesPlugin.getWorkspace().getRoot()
                                                .findMember(platformString);
                                        if (workspaceResource != null) {
                                            project = workspaceResource.getProject();
                                        }
                                    }
                                }
                                if (project != null) {
                                    Collection<Session> sessions = SessionManager.INSTANCE.getSessions();
                                    for (Session session : sessions) {
                                        URI sessionResourceURI = session.getSessionResource().getURI();
                                        IFile sessionFile = ResourcesPlugin.getWorkspace().getRoot()
                                                .getFile(new Path(sessionResourceURI.toPlatformString(true)));
                                        if (sessionFile != null && sessionFile.getProject().equals(project)) {
                                            modelPath = sessionFile.getFullPath().toPortableString();
                                        }
                                    }
                                }
                                EObject currentElement = (EObject) obj;
                                while (currentElement != null && !(currentElement instanceof SystemEngineering)) {
                                    currentElement = currentElement.eContainer();
                                }
                                if (currentElement instanceof SystemEngineering) {
                                    SystemEngineering systemEngineering = (SystemEngineering) currentElement;
                                    modelId = systemEngineering.getId();
                                }
                                if (obj instanceof ModelElement) {
                                    ModelElement element = (ModelElement) obj;
                                    label = element.getLabel();
                                    id = element.getId();
                                } else if (obj instanceof Module) {
                                    Module element = (Module) obj;
                                    label = element.getReqIFName();
                                    id = element.getId();
                                } else if (obj instanceof RecCatalog) {
                                    RecCatalog element = (RecCatalog) obj;
                                    label = element.getName();
                                    id = element.getId();
                                } else if (obj instanceof Requirement) {
                                    Requirement element = (Requirement) obj;
                                    label = element.getReqIFName();
                                    id = element.getId();
                                } else if (obj instanceof DRepresentationDescriptor) {
                                    DRepresentationDescriptor element = (DRepresentationDescriptor) obj;
                                    label = element.getName();
                                    id = element.getUid();
                                }
                                SelectedElement selectedElement = new SelectedElement(id, label, modelId, modelPath);
                                selectedElementList.addElement(selectedElement);
                            }
                            boolean selectionChanged = !selectedElementList.equals(prevSelectedElementList);
                            if (selectionChanged) {
                                ObjectMapper mapper = new ObjectMapper();
                                String json = mapper.writeValueAsString(selectedElementList);
                                String targetUrl = selectedElementsServiceTargetUrl + "?capellacollab_session_id="
                                        + capellaCollabSessionId;
                                sendPostRequest(targetUrl, json);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println(e.getMessage());
                        }
                        prevSelectedElementList.setElements(selectedElementList.getElements());
                    }
                });
            }
        };
        timer.scheduleAtFixedRate(task, 0, 500);
    }
}
