/*******************************************************************************
 * Copyright (c) 2016-2017 Rogue Wave Software Inc. and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Michał Niewrzał (Rogue Wave Software Inc.) - initial implementation
 *  Mickael Istria (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.lsp4e.test.edit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.lsp4e.LanguageServiceAccessor;
import org.eclipse.lsp4e.test.TestUtils;
import org.eclipse.lsp4e.tests.mock.MockLanguageSever;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DocumentDidChangeTest {

	private IProject project;

	@Before
	public void setUp() throws CoreException {
		project =  TestUtils.createProject("DocumentDidChangeTest"+System.currentTimeMillis());
	}

	@After
	public void tearDown() throws CoreException {
		project.delete(true, true, new NullProgressMonitor());
		MockLanguageSever.INSTANCE.shutdown();
	}
	
	@Test
	public void testIncrementalSync() throws Exception {
		MockLanguageSever.INSTANCE.getInitializeResult().getCapabilities()
				.setTextDocumentSync(TextDocumentSyncKind.Incremental);

		IFile testFile = TestUtils.createUniqueTestFile(project, "");
		IEditorPart editor = TestUtils.openEditor(testFile);
		ITextViewer viewer = TestUtils.getTextViewer(editor);
		LanguageServiceAccessor.getInitializedLanguageServers(testFile, new Predicate<ServerCapabilities>() {
			@Override
			public boolean test(ServerCapabilities t) {
				TextDocumentSyncKind syncKind = getDocumentSyncKind(t);
				assertEquals(TextDocumentSyncKind.Incremental, syncKind);
				return true;
			}
		});

		// Test initial insert
		CompletableFuture<DidChangeTextDocumentParams> didChangeExpectation = new CompletableFuture<DidChangeTextDocumentParams>();
		MockLanguageSever.INSTANCE.setDidChangeCallback(didChangeExpectation);
		viewer.getDocument().replace(0, 0, "Hello");
		DidChangeTextDocumentParams lastChange = didChangeExpectation.get(1000, TimeUnit.MILLISECONDS);
		assertNotNull(lastChange.getContentChanges());
		assertEquals(1, lastChange.getContentChanges().size());
		TextDocumentContentChangeEvent change0 = lastChange.getContentChanges().get(0);
		Range range = change0.getRange();
		assertNotNull(range);
		assertEquals(0, range.getStart().getLine());
		assertEquals(0, range.getStart().getCharacter());
		assertEquals(0, range.getEnd().getLine());
		assertEquals(0, range.getEnd().getCharacter());
		assertEquals(Integer.valueOf(0), change0.getRangeLength());
		assertEquals("Hello", change0.getText());

		// Test additional insert
		didChangeExpectation = new CompletableFuture<DidChangeTextDocumentParams>();
		MockLanguageSever.INSTANCE.setDidChangeCallback(didChangeExpectation);
		viewer.getDocument().replace(5, 0, " ");
		lastChange = didChangeExpectation.get(1000, TimeUnit.MILLISECONDS);
		assertNotNull(lastChange.getContentChanges());
		assertEquals(1, lastChange.getContentChanges().size());
		change0 = lastChange.getContentChanges().get(0);
		range = change0.getRange();
		assertNotNull(range);
		assertEquals(0, range.getStart().getLine());
		assertEquals(5, range.getStart().getCharacter());
		assertEquals(0, range.getEnd().getLine());
		assertEquals(5, range.getEnd().getCharacter());
		assertEquals(Integer.valueOf(0), change0.getRangeLength());
		assertEquals(" ", change0.getText());

		// test replace
		didChangeExpectation = new CompletableFuture<DidChangeTextDocumentParams>();
		MockLanguageSever.INSTANCE.setDidChangeCallback(didChangeExpectation);
		viewer.getDocument().replace(0, 5, "Hallo");
		lastChange = didChangeExpectation.get(1000, TimeUnit.MILLISECONDS);
		assertNotNull(lastChange.getContentChanges());
		assertEquals(1, lastChange.getContentChanges().size());
		change0 = lastChange.getContentChanges().get(0);
		range = change0.getRange();
		assertNotNull(range);
		assertEquals(0, range.getStart().getLine());
		assertEquals(0, range.getStart().getCharacter());
		assertEquals(0, range.getEnd().getLine());
		assertEquals(5, range.getEnd().getCharacter());
		assertEquals(Integer.valueOf(5), change0.getRangeLength());
		assertEquals("Hallo", change0.getText());

		((AbstractTextEditor)editor).close(false);
	}

	@Test
	public void testIncrementalSync_deleteLastLine() throws Exception {
		MockLanguageSever.INSTANCE.getInitializeResult().getCapabilities()
				.setTextDocumentSync(TextDocumentSyncKind.Incremental);

		String multiLineText = "line1\nline2\nline3\n";
		IFile testFile = TestUtils.createUniqueTestFile(project, multiLineText);
		IEditorPart editor = TestUtils.openEditor(testFile);
		ITextViewer viewer = TestUtils.getTextViewer(editor);
		LanguageServiceAccessor.getInitializedLanguageServers(testFile, new Predicate<ServerCapabilities>() {
			@Override
			public boolean test(ServerCapabilities t) {
				assertEquals(TextDocumentSyncKind.Incremental, getDocumentSyncKind(t));
				return true;
			}
		});

		// Test initial insert
		CompletableFuture<DidChangeTextDocumentParams> didChangeExpectation = new CompletableFuture<DidChangeTextDocumentParams>();
		MockLanguageSever.INSTANCE.setDidChangeCallback(didChangeExpectation);
		viewer.getDocument().replace("line1\nline2\n".length(), "line3\n".length(), "");
		DidChangeTextDocumentParams lastChange = didChangeExpectation.get(1000, TimeUnit.MILLISECONDS);
		assertNotNull(lastChange.getContentChanges());
		assertEquals(1, lastChange.getContentChanges().size());
		TextDocumentContentChangeEvent change0 = lastChange.getContentChanges().get(0);
		Range range = change0.getRange();
		assertNotNull(range);
		assertEquals(2, range.getStart().getLine());
		assertEquals(0, range.getStart().getCharacter());
		assertEquals(3, range.getEnd().getLine());
		assertEquals(0, range.getEnd().getCharacter());
		assertEquals(Integer.valueOf(6), change0.getRangeLength());
		assertEquals("", change0.getText());

		((AbstractTextEditor)editor).close(false);
	}

	@Test
	public void testFullSync() throws Exception {
		MockLanguageSever.INSTANCE.getInitializeResult().getCapabilities()
				.setTextDocumentSync(TextDocumentSyncKind.Full);

		IFile testFile = TestUtils.createUniqueTestFile(project, "");
		IEditorPart editor = TestUtils.openEditor(testFile);
		ITextViewer viewer = TestUtils.getTextViewer(editor);
		LanguageServiceAccessor.getInitializedLanguageServers(testFile, new Predicate<ServerCapabilities>() {
			@Override
			public boolean test(ServerCapabilities t) {
				assertEquals(TextDocumentSyncKind.Full, getDocumentSyncKind(t));
				return true;
			}
		});
		// Test initial insert
		CompletableFuture<DidChangeTextDocumentParams> didChangeExpectation = new CompletableFuture<DidChangeTextDocumentParams>();
		MockLanguageSever.INSTANCE.setDidChangeCallback(didChangeExpectation);
		String text = "Hello";
		viewer.getDocument().replace(0, 0, text);
		DidChangeTextDocumentParams lastChange = didChangeExpectation.get(1000, TimeUnit.MILLISECONDS);
		assertNotNull(lastChange.getContentChanges());
		assertEquals(1, lastChange.getContentChanges().size());
		TextDocumentContentChangeEvent change0 = lastChange.getContentChanges().get(0);
		assertEquals(text, change0.getText());

		// Test additional insert
		didChangeExpectation = new CompletableFuture<DidChangeTextDocumentParams>();
		MockLanguageSever.INSTANCE.setDidChangeCallback(didChangeExpectation);
		viewer.getDocument().replace(5, 0, " World");
		lastChange = didChangeExpectation.get(1000, TimeUnit.MILLISECONDS);
		assertNotNull(lastChange.getContentChanges());
		assertEquals(1, lastChange.getContentChanges().size());
		change0 = lastChange.getContentChanges().get(0);
		assertEquals("Hello World", change0.getText());
		
		((AbstractTextEditor)editor).close(false);
	}

	private TextDocumentSyncKind getDocumentSyncKind(ServerCapabilities t) {
		TextDocumentSyncKind syncKind = null;
		if (t.getTextDocumentSync().isLeft()) {
			syncKind = t.getTextDocumentSync().getLeft();
		} else if (t.getTextDocumentSync().isRight()) {
			syncKind = t.getTextDocumentSync().getRight().getChange();
		}
		return syncKind;
	}
}
