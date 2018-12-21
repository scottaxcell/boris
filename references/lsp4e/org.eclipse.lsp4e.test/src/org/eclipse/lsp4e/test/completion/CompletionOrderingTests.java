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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.lsp4e.LanguageServiceAccessor;
import org.eclipse.lsp4e.LanguageServiceAccessor.LSPDocumentInfo;
import org.eclipse.lsp4e.operations.completion.LSIncompleteCompletionProposal;
import org.eclipse.lsp4e.test.TestUtils;
import org.eclipse.lsp4e.tests.mock.MockLanguageSever;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.junit.Test;

public class CompletionOrderingTests extends AbstractCompletionTest {

	@Test
	public void testItemOrdering() throws Exception {
		confirmCompletionResults(new String[] { "AA", "AB", "BA", "BB", "CB", "CC" }, "B", 1,
				new String[] { "BA", "BB", "AB", "CB" });
	}

	@Test
	public void testOrderByCategory() throws Exception {
		// Category 1 before Category 2 (testa)
		String[] completions = new String[] { "testa", "test.a", "a.test.a", "a.testa", "test" };
		String[] orderedResults = new String[] { "test", "test.a", "a.test.a", "testa", "a.testa" };
		confirmCompletionResults(completions, "test", 4, orderedResults);

		// Category 2 before Category 3 (atest)
		completions = new String[] { "testa", "atest", "a.testa" };
		orderedResults = new String[] { "testa", "a.testa", "atest" };
		confirmCompletionResults(completions, "test", 4, orderedResults);

		// Category 3 before Category 4 (tZesZt)
		completions = new String[] { "atesta", "tZesZt", "atest" };
		orderedResults = new String[] { "atest", "atesta", "tZesZt" };
		confirmCompletionResults(completions, "test", 4, orderedResults);

		// Category 4 before Category 5 (qwerty)
		completions = new String[] { "qwerty", "tZesZt", "t.e.s.t" };
		orderedResults = new String[] { "tZesZt", "t.e.s.t", "qwerty" };
		confirmCompletionResults(completions, "test", 4, orderedResults);
	}

	@Test
	public void testOrderByRank() throws Exception {
		// Category 1
		String[] completions = new String[] { "prefix.test", "alongprefix.test", "test", "test.test", "pretest.test" };
		String[] orderedResults = new String[] { "test", "test.test", "prefix.test", "pretest.test",
				"alongprefix.test" };
		confirmCompletionResults(completions, "test", 4, orderedResults);

		// Category 2
		completions = new String[] { "testa", "alongprefix.testa", "testatest", "prefix.testa" };
		orderedResults = new String[] { "testa", "testatest", "prefix.testa", "alongprefix.testa" };
		confirmCompletionResults(completions, "test", 4, orderedResults);

		// Category 3
		completions = new String[] { "atesta", "teteteststst", "long.prefixtest.suffix" };
		orderedResults = new String[] { "atesta", "teteteststst", "long.prefixtest.suffix" };
		confirmCompletionResults(completions, "test", 4, orderedResults);

		// Category 4
		completions = new String[] { "tlongbreakbetweenest", "tZesZt", "t.e.s.t", "tes.tst" };
		orderedResults = new String[] { "tes.tst", "tZesZt", "t.e.s.t", "tlongbreakbetweenest" };
		confirmCompletionResults(completions, "test", 4, orderedResults);
	}

	@Test
	public void testOrderWithCapitalization() throws Exception {
		// Category 1
		String[] completions = new String[] { "prefiX.Test", "alongprefix.test", "tEsT", "teSt.teST", "preTEst.test" };
		String[] orderedResults = new String[] { "tEsT", "teSt.teST", "prefiX.Test", "preTEst.test",
				"alongprefix.test" };
		confirmCompletionResults(completions, "test", 4, orderedResults);

		// Category 2
		completions = new String[] { "teSTa", "alonGPrefix.TESTA", "tEStatest", "prefix.testa" };
		orderedResults = new String[] { "teSTa", "tEStatest", "prefix.testa", "alonGPrefix.TESTA" };
		confirmCompletionResults(completions, "tESt", 4, orderedResults);

		// Category 3
		completions = new String[] { "ATesta", "teTETesTSTst", "long.prefixtest.suffix" };
		orderedResults = new String[] { "ATesta", "teTETesTSTst", "long.prefixtest.suffix" };
		confirmCompletionResults(completions, "TEST", 4, orderedResults);

		// Category 4
		completions = new String[] { "TlongbreakbetweenEST", "TZesZT", "t.e.s.t", "teS.tst" };
		orderedResults = new String[] { "teS.tst", "TZesZT", "t.e.s.t", "TlongbreakbetweenEST" };
		confirmCompletionResults(completions, "test", 4, orderedResults);
	}

	@Test
	public void testOrderWithLongInsert() throws Exception {
		List<CompletionItem> items = new ArrayList<>();
		CompletionItem item = new CompletionItem("server.address");
		item.setFilterText("server.address");
		item.setTextEdit(new TextEdit(new Range(new Position(1, 12), new Position(5, 7)),
						"  address: $1\n" +
						"spring:\n" +
						"  application:\n" +
						"    name: f\n"));
		items.add(item);

		item = new CompletionItem("management.server.address");
		item.setFilterText("management.server.address");
		item.setTextEdit(new TextEdit(new Range(new Position(5, 0), new Position(5, 7)),
						"management:\n" +
						"  server:\n" +
						"    address: $1\n"));
		items.add(item);

		item = new CompletionItem("→ spring.jta.atomikos.datasource.xa-data-source-class-name");
		item.setFilterText("spring.jta.atomikos.datasource.xa-data-source-class-name");
		item.setTextEdit(new TextEdit(new Range(new Position(5, 0), new Position(0, 12)),item.getFilterText()));
		items.add(item);

		String[] orderedResults = new String[] { "server.address", "management.server.address",
				"→ spring.jta.atomikos.datasource.xa-data-source-class-name" };

		confirmCompletionResults(items,
						"server:\n" +
						"  port: 555\n" +
						"spring:\n" +
						"  application:\n" +
						"    name: f\n" +
						"address",
				62, orderedResults);
	}

	@Test
	public void testMovingOffset() throws Exception {
		Range range = new Range(new Position(0, 0), new Position(0, 4));
		IDocument document = TestUtils.openTextViewer(TestUtils.createUniqueTestFile(project, "test")).getDocument();
		LSPDocumentInfo info = LanguageServiceAccessor
				.getLSPDocumentInfosFor(document,
						capabilities -> capabilities.getCompletionProvider() != null
						|| capabilities.getSignatureHelpProvider() != null)
				.get(0);

		CompletionItem completionItem = createCompletionItem("test", CompletionItemKind.Class, range);
		LSIncompleteCompletionProposal completionProposal = new LSIncompleteCompletionProposal(completionItem, 0, info);
		// Blank input ''
		assertEquals("", completionProposal.getDocumentFilter());
		assertEquals(0, completionProposal.getRankScore());
		assertEquals(5, completionProposal.getRankCategory());
		// Typed test 'test'
		assertEquals("test", completionProposal.getDocumentFilter(4));
		assertEquals(0, completionProposal.getRankScore());
		assertEquals(1, completionProposal.getRankCategory());
		// Moved cursor back 't'
		assertEquals("t", completionProposal.getDocumentFilter(1));
		assertEquals(0, completionProposal.getRankScore());
		assertEquals(2, completionProposal.getRankCategory());

		document.set("prefix:pnd");
		completionItem = createCompletionItem("append", CompletionItemKind.Class);
		completionProposal = new LSIncompleteCompletionProposal(completionItem, 7, info);
		// Blank input 'prefix:'
		assertEquals("", completionProposal.getDocumentFilter());
		assertEquals(0, completionProposal.getRankScore());
		assertEquals(5, completionProposal.getRankCategory());
		// Typed test 'prefix:pnd'
		assertEquals("pnd", completionProposal.getDocumentFilter(10));
		assertEquals(5, completionProposal.getRankScore());
		assertEquals(4, completionProposal.getRankCategory());
		// Moved cursor back 'prefix:p'
		assertEquals("p", completionProposal.getDocumentFilter(8));
		assertEquals(1, completionProposal.getRankScore());
		assertEquals(3, completionProposal.getRankCategory());
	}

	@Test
	public void testPerformance() throws Exception {
		final int[] batchSizes = new int[] { 10, 100, 1000, 10000 };
		int[] resultAverages = new int[batchSizes.length];
		final int repeat = 5;
		final ITextViewer viewer = TestUtils.openTextViewer(TestUtils.createUniqueTestFile(project, "abcdefgh"));

		for (int i = 0; i < batchSizes.length; i++) {
			long resultSum = 0;
			for (int j = 0; j < repeat; j++) {
				resultSum += timeToDisplayCompletionList(viewer, batchSizes[i]);
			}
			resultAverages[i] = (int) (resultSum / repeat);
		}
		double pearsonCorrelation = isLinearCorelation(batchSizes, resultAverages);
		assertTrue(pearsonCorrelation > 0.99);
	}

	private double isLinearCorelation(int[] batchSizes, int[] resultAverages) {
		int n = batchSizes.length;

		long batchSum = 0;
		long resulthSum = 0;
		long batchSumSquared = 0;
		long resulthSumSquared = 0;
		long productSum = 0;
		for (int i = 0; i < n; i++) {
			batchSum += batchSizes[i];
			resulthSum += resultAverages[i];
			batchSumSquared += Math.pow(batchSizes[i], 2);
			resulthSumSquared += Math.pow(resultAverages[i], 2);
			productSum += batchSizes[i] * resultAverages[i];
		}
		double numerator = productSum - (batchSum * resulthSum / n);
		double denominator = Math.sqrt(
				(batchSumSquared - Math.pow(batchSum, 2) / n) * (resulthSumSquared - Math.pow(resulthSum, 2) / n));
		return denominator == 0 ? 0 : numerator / denominator;
	}

	private long timeToDisplayCompletionList(ITextViewer viewer, int size) throws Exception {
		Range range = new Range(new Position(0, 0), new Position(0, 8));
		List<CompletionItem> items = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			items.add(createCompletionItem(randomString(), CompletionItemKind.Class, range));
		}
		MockLanguageSever.INSTANCE.setCompletionList(new CompletionList(false, items));

		long startTimeControl = System.currentTimeMillis();
		contentAssistProcessor.computeCompletionProposals(viewer, 0);
		long endTimeControl = System.currentTimeMillis();
		return endTimeControl - startTimeControl;
	}

	private static final String CHARACTERS = "abcdefghABCDEFGH.-_";

	private String randomString() {
		int count = 50;
		StringBuilder builder = new StringBuilder();
		while (count-- != 0) {
			int character = (int) (Math.random() * CHARACTERS.length());
			builder.append(CHARACTERS.charAt(character));
		}
		return builder.toString();
	}
}
