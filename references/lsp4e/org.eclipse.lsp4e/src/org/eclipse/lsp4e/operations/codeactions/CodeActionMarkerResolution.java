/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Mickael Istria (Red Hat Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.lsp4e.operations.codeactions;

import java.util.Arrays;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4e.LanguageServerPlugin;
import org.eclipse.lsp4e.operations.diagnostics.LSPDiagnosticsToMarkers;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;

public class CodeActionMarkerResolution extends WorkbenchMarkerResolution implements IMarkerResolution {

	private CodeAction codeAction;

	public CodeActionMarkerResolution(CodeAction codeAction) {
		this.codeAction = codeAction;
	}

	@Override
	public String getDescription() {
		return codeAction.getTitle();
	}

	@Override
	public Image getImage() {
		return null;
	}

	@Override
	public String getLabel() {
		return codeAction.getTitle();
	}

	@Override
	public void run(IMarker marker) {
		if (codeAction.getEdit() != null) {
			LSPEclipseUtils.applyWorkspaceEdit(codeAction.getEdit());
		}
		if (codeAction.getCommand() != null) {
			CommandMarkerResolution.executeCommand(codeAction.getCommand(), marker.getResource(),
					marker.getAttribute(LSPDiagnosticsToMarkers.LANGUAGE_SERVER_ID, null));
		}
	}

	@Override
	public IMarker[] findOtherMarkers(IMarker[] markers) {
		if (markers == null) {
			return new IMarker[0];
		}
		return Arrays.stream(markers).filter(marker -> {
			try {
				return codeAction.getDiagnostics()
						.contains(marker.getAttribute(LSPDiagnosticsToMarkers.LSP_DIAGNOSTIC));
			} catch (CoreException e) {
				LanguageServerPlugin.logError(e);
				return false;
			}
		}).toArray(IMarker[]::new);
	}

}
