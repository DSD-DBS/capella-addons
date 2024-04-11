import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IStartup;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class Main implements IStartup {
    public static void log(int severity, String message, Throwable exception) {
        Bundle bundle = FrameworkUtil.getBundle(Main.class);
        ILog log = Platform.getLog(bundle);
        IStatus status = new Status(severity, bundle.getSymbolicName(),
                message, exception);
        log.log(status);
    }

    private void importProject(File projectDir) {
        if (!projectDir.isDirectory()) {
            String msg = "Directory '" + projectDir.getName() + "' does not exist.";
            log(IStatus.WARNING, msg, null);
            return;
        }
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        File dotProjectFile = new File(projectDir, ".project");
        if (!dotProjectFile.exists()) {
            String msg = "Folder '" + projectDir.getPath() + "' does not ";
            msg += "contain .project file. The project cannot be imported.";
            log(IStatus.WARNING, msg, null);
            return;
        }
        IPath projectLocation = new Path(dotProjectFile.getAbsolutePath());
        try {
            IProjectDescription projectDescription = workspace
                    .loadProjectDescription(projectLocation);
            IProject project = workspace.getRoot().getProject(projectDescription.getName());
            if (project.exists()) {
                String msg = "Project '" + projectDir.getName() + "' does already exist.";
                log(IStatus.INFO, msg, null);
                return;
            }
            project.create(projectDescription, null);
            String msg = "Imported project '" + projectDir.getName() + "'";
            log(IStatus.INFO, msg, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void earlyStartup() {
        // Check if the MODEL_INBOX_DIRECTORIES env var is set and read it
        String modelInboxDirectories_string;
        modelInboxDirectories_string = System.getenv("MODEL_INBOX_DIRECTORIES");
        if (modelInboxDirectories_string == null) {
            String msg = "MODEL_INBOX_DIRECTORIES environment variable is not set.";
            log(IStatus.INFO, msg, null);
            return;
        }
        // If the modelInboxDirectories contains a colon, split the
        // modelInboxDirectories string into an array of directories which are
        // separated by a colon. Otherwise, create a String array with one item
        // that equals the modelInboxDirectories_string
        String[] modelDirectories;
        if (modelInboxDirectories_string.contains(":")) {
            modelDirectories = modelInboxDirectories_string.split(":");
        } else {
            modelDirectories = new String[] { modelInboxDirectories_string };
        }
        for (String dir : modelDirectories) {
            importProject(new File(dir));
        }
    }
}
