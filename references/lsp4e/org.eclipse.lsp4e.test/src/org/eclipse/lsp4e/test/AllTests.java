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
 *  Mickael Istria (Red Hat Inc.) - Added some suites
 *******************************************************************************/
package org.eclipse.lsp4e.test;

import org.eclipse.lsp4e.test.codeactions.CodeActionTests;
import org.eclipse.lsp4e.test.commands.DynamicRegistrationTest;
import org.eclipse.lsp4e.test.completion.CompleteCompletionTest;
import org.eclipse.lsp4e.test.completion.CompletionOrderingTests;
import org.eclipse.lsp4e.test.completion.ContextInformationTest;
import org.eclipse.lsp4e.test.completion.IncompleteCompletionTest;
import org.eclipse.lsp4e.test.definition.DefinitionTest;
import org.eclipse.lsp4e.test.diagnostics.DiagnosticsTest;
import org.eclipse.lsp4e.test.documentLink.DocumentLinkTest;
import org.eclipse.lsp4e.test.edit.DocumentDidChangeTest;
import org.eclipse.lsp4e.test.edit.DocumentDidCloseTest;
import org.eclipse.lsp4e.test.edit.DocumentDidOpenTest;
import org.eclipse.lsp4e.test.edit.DocumentDidSaveTest;
import org.eclipse.lsp4e.test.edit.DocumentRevertAndCloseTest;
import org.eclipse.lsp4e.test.edit.LSPEclipseUtilsTest;
import org.eclipse.lsp4e.test.format.FormatTest;
import org.eclipse.lsp4e.test.highlight.HighlightTest;
import org.eclipse.lsp4e.test.hover.HoverTest;
import org.eclipse.lsp4e.test.references.FindReferencesTest;
import org.eclipse.lsp4e.test.rename.RenameTest;
import org.eclipse.lsp4e.test.symbols.SymbolsModelTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	LanguageServiceAccessorTest.class,
	ContentTypeToLanguageServerDefinitionTest.class,
	CompleteCompletionTest.class,
	IncompleteCompletionTest.class,
	CompletionOrderingTests.class,
	ContextInformationTest.class,
	DocumentDidOpenTest.class,
	DocumentDidChangeTest.class,
	DocumentDidSaveTest.class,
	DocumentDidCloseTest.class,
	DocumentRevertAndCloseTest.class,
	SymbolsModelTest.class,
	LSPEclipseUtilsTest.class,
	HoverTest.class,
	DefinitionTest.class,
	DiagnosticsTest.class,
	FindReferencesTest.class,
	FormatTest.class,
	CodeActionTests.class,
	DocumentLinkTest.class,
	RunningLanguageServerTest.class,
	HighlightTest.class,
		DynamicRegistrationTest.class, RenameTest.class
})
public class AllTests {

}
