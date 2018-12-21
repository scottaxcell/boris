/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Rastislav Wagner (Red Hat Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.lsp4e.ui;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.lsp4e.ContentTypeToLanguageServerDefinition;
import org.eclipse.lsp4e.LanguageServersRegistry.LanguageServerDefinition;
import org.eclipse.lsp4e.LanguageServiceAccessor;
import org.eclipse.ui.IEditorReference;

public class EnableDisableLSJob extends Job {

	private List<ContentTypeToLanguageServerDefinition> serverDefinitions;
	private IEditorReference[] editors;

	public EnableDisableLSJob(List<ContentTypeToLanguageServerDefinition> serverDefinitions,
			IEditorReference[] editors) {
		super(Messages.enableDisableLSJob);
		this.serverDefinitions = serverDefinitions;
		this.editors = editors;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		for (ContentTypeToLanguageServerDefinition changedDefinition : serverDefinitions) {
			LanguageServerDefinition serverDefinition = changedDefinition.getValue();
			if (serverDefinition != null) {
				if (!changedDefinition.isEnabled()) {
					LanguageServiceAccessor.disableLanguageServerContentType(changedDefinition);
				} else if (editors != null) {
					LanguageServiceAccessor.enableLanguageServerContentType(changedDefinition, editors);
				}
			}
		}
		return Status.OK_STATUS;
	}

}
