// Copyright DB InfraGO AG and contributors
// SPDX-License-Identifier: Apache-2.0

import org.eclipse.ui.IStartup;

public class Main implements IStartup {
    /**
     * Will be called in a separate thread after the workbench initializes.
     * <p>
     * Note that most workbench methods must be called in the UI thread since they
     * may access SWT. For example, to obtain the current workbench window, use:
     * </p>
     *
     * <pre>
     * <code>
     * IWorkbench workbench = PlatformUI.getWorkbench();
     * workbench.getDisplay().asyncExec(new Runnable() {
     *   public void run() {
     *     IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
     *     if (window != null) {
     *       // do something
     *     }
     *   }
     * });
     * </code>
     * </pre>
     *
     * @see org.eclipse.swt.widgets.Display#asyncExec
     * @see org.eclipse.swt.widgets.Display#syncExec
     */
    @Override
    public void earlyStartup() {
        System.out.println("earlyStartup() called");
    }
}
