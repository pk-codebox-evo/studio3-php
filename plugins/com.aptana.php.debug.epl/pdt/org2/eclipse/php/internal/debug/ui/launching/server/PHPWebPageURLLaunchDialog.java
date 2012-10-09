/*******************************************************************************
 * Copyright (c) 2006 Zend Corporation and IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Zend and IBM - Initial implementation
 *******************************************************************************/
package org2.eclipse.php.internal.debug.ui.launching.server;

import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

import com.aptana.php.debug.core.IPHPDebugCorePreferenceKeys;
import com.aptana.php.debug.epl.PHPDebugEPLPlugin;
import com.aptana.webserver.core.IServer;

/**
 * The PHPWebPageURLLaunchDialog is a dialog that will be displayed for the user when a server launch
 * shortcut is triggered.
 * The dialog hides all the default values and let the user to set the page URL that will be used to
 * initialize the debug session.
 * This dialog appears only when the launch is a NEW lauch. In cases where we already identified that
 * there is a launch configuration on the selected resource (file) we use the configuration without
 * asking.
 *
 * @author shalom
 */
public class PHPWebPageURLLaunchDialog extends MessageDialog {

	private static Set previousURLs = new TreeSet();
	private ILaunchConfigurationWorkingCopy launchConfiguration;
	private Combo combo;
	private final IServer server;

	/**
	 * Constructs a new ServerURLLaunchDialog.
	 *
	 * @param launchConfiguration The launch configuration the we are using for the current launch.
	 * @param parentShell
	 * @param dialogTitle
	 * @param dialogTitleImage
	 * @param dialogMessage
	 * @param dialogImageType
	 * @param dialogButtonLabels
	 * @param defaultIndex
	 */
	public PHPWebPageURLLaunchDialog(ILaunchConfigurationWorkingCopy launchConfiguration, IServer server, String title) {
		super(PHPDebugEPLPlugin.getActiveWorkbenchShell(), title, null, "", INFORMATION, new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 0);
		this.launchConfiguration = launchConfiguration;
		this.server = server;
		message = "Note that no files will be published to the server.";
	}

	/**
	 * Create a drop-down list for the URL.
	 */
	protected Control createCustomArea(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText("Launch URL");
		group.setLayout(new GridLayout(1, true));
		group.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
		combo = new Combo(group, SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1);
		data.widthHint = convertWidthInCharsToPixels(80);
		combo.setLayoutData(data);
		Object[] urls = previousURLs.toArray();
		for (Object element : urls) {
			combo.add(element.toString());
		}
		try {
			String selectedURL = launchConfiguration.getAttribute(IPHPDebugCorePreferenceKeys.ATTR_SERVER_BASE_URL, "");
			int comboIndex = combo.indexOf(selectedURL);
			if (comboIndex > -1) {
				combo.select(comboIndex);
			} else {
				combo.add(selectedURL, 0);
				combo.select(0);
			}
		} catch (CoreException e) {
			PHPDebugEPLPlugin.logError(e);
		}

		return parent;
	}

	/**
	 * Override the okPressed to save the URL to the URLs history for this PHP IDE session.
	 * Also, add the URL to the launch configuration attributes.
	 */
	protected void buttonPressed(int buttonId) {
		if (buttonId == OK) {
			String url = combo.getText().trim();
			previousURLs.add(url);
			try {
				if (!url.equals(launchConfiguration.getAttribute(IPHPDebugCorePreferenceKeys.ATTR_SERVER_BASE_URL, ""))) {
					// In case that the user manually changed the URL, make sure that we flag it as a non-AutoGenerated URL.
					launchConfiguration.setAttribute(IPHPDebugCorePreferenceKeys.ATTR_AUTO_GENERATED_URL, false);
				}
			} catch (CoreException e) {
				PHPDebugEPLPlugin.logError(e);
				// And just to make sure, make this configuration without an Auto-Generated URL.
				launchConfiguration.setAttribute(IPHPDebugCorePreferenceKeys.ATTR_AUTO_GENERATED_URL, false);
			}
			launchConfiguration.setAttribute(IPHPDebugCorePreferenceKeys.ATTR_SERVER_BASE_URL, url);
		}
		super.buttonPressed(buttonId);
	}

}
