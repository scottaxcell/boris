/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Lucas Bullen (Red Hat Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.lsp4e.test.completion;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.lsp4e.operations.completion.LSContentAssistProcessor;
import org.eclipse.lsp4e.test.TestUtils;
import org.eclipse.lsp4e.tests.mock.MockLanguageSever;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.junit.After;
import org.junit.Before;

public abstract class AbstractCompletionTest {
	protected IProject project;
	protected LSContentAssistProcessor contentAssistProcessor;

	@Before
	public void setUp() throws CoreException {
		project = TestUtils.createProject("CompletionTest" + System.currentTimeMillis());
		contentAssistProcessor = new LSContentAssistProcessor();
	}

	@After
	public void tearDown() throws CoreException {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(false);
		project.delete(true, true, new NullProgressMonitor());
		MockLanguageSever.INSTANCE.shutdown();
	}

	protected CompletionItem createCompletionItem(String label, CompletionItemKind kind) {
		return createCompletionItem(label, kind, new Range(new Position(0, 0), new Position(0, label.length())));
	}

	protected CompletionItem createCompletionItem(String label, CompletionItemKind kind, Range range) {
		CompletionItem item = new CompletionItem();
		item.setLabel(label);
		item.setKind(kind);
		item.setTextEdit(new TextEdit(range, label));
		return item;
	}

	protected void confirmCompletionResults(String[] completions, String content, Integer cursorIndexInContent,
			String[] expectedOrder) throws PartInitException, InvocationTargetException, CoreException {
		Range range = new Range(new Position(0, 0), new Position(0, cursorIndexInContent));
		List<CompletionItem> items = new ArrayList<>();
		for (String string : completions) {
			items.add(createCompletionItem(string, CompletionItemKind.Class, range));
		}
		confirmCompletionResults(items, content, cursorIndexInContent, expectedOrder);
	}

	protected void confirmCompletionResults(List<CompletionItem> completions, String content,
			Integer cursorIndexInContent, String[] expectedOrder)
			throws PartInitException, InvocationTargetException, CoreException {

		MockLanguageSever.INSTANCE.setCompletionList(new CompletionList(false, completions));
		ITextViewer viewer = TestUtils.openTextViewer(TestUtils.createUniqueTestFile(project, content));

		ICompletionProposal[] proposals = contentAssistProcessor.computeCompletionProposals(viewer,
				cursorIndexInContent);
		assertEquals(expectedOrder.length, proposals.length);
		for (int i = 0; i < proposals.length; i++) {
			assertEquals(expectedOrder[0], proposals[0].getDisplayString());
		}
	}
}
