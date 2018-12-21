/*******************************************************************************
 * Copyright (c) 2017 Rogue Wave Software Inc. and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Michał Niewrzał (Rogue Wave Software Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.lsp4e.test.highlight;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.tests.util.DisplayHelper;
import org.eclipse.lsp4e.operations.highlight.HighlightReconcilingStrategy;
import org.eclipse.lsp4e.test.TestUtils;
import org.eclipse.lsp4e.tests.mock.MockLanguageSever;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.swt.widgets.Display;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class HighlightTest {

	private IProject project;

	@Before
	public void setUp() throws CoreException {
		project = TestUtils.createProject("HighlightTest" + System.currentTimeMillis());
	}

	@After
	public void tearDown() throws CoreException {
		project.delete(true, true, new NullProgressMonitor());
		MockLanguageSever.INSTANCE.shutdown();
	}

	@Test
	public void testHighlight() throws CoreException, InvocationTargetException {
		checkGenericEditorVersion();

		List<DocumentHighlight> highlights = new ArrayList<>();
		highlights.add(
				new DocumentHighlight(new Range(new Position(0, 2), new Position(0, 6)), DocumentHighlightKind.Read));
		highlights.add(
				new DocumentHighlight(new Range(new Position(0, 7), new Position(0, 12)), DocumentHighlightKind.Write));
		highlights.add(
				new DocumentHighlight(new Range(new Position(0, 13), new Position(0, 17)), DocumentHighlightKind.Text));
		MockLanguageSever.INSTANCE.setDocumentHighlights(highlights);

		IFile testFile = TestUtils.createUniqueTestFile(project, "  READ WRITE TEXT");
		ITextViewer viewer = TestUtils.openTextViewer(testFile);

		viewer.getTextWidget().setCaretOffset(1);

		if (!(viewer instanceof ISourceViewer)) {
			Assert.fail();
		}

		ISourceViewer sourceViewer = (ISourceViewer) viewer;

		Map<org.eclipse.jface.text.Position, Annotation> annotations = new HashMap<>();

		new DisplayHelper() {
			@Override
			protected boolean condition() {
				return sourceViewer.getAnnotationModel().getAnnotationIterator().hasNext();
			}
		}.waitForCondition(Display.getCurrent(), 3000);

		IAnnotationModel model = sourceViewer.getAnnotationModel();
		final Iterator<Annotation> iterator = model.getAnnotationIterator();
		while (iterator.hasNext()) {
			Annotation annotation = iterator.next();
			annotations.put(model.getPosition(annotation), annotation);
		}

		Annotation annotation = annotations.get(new org.eclipse.jface.text.Position(2, 4));
		Assert.assertNotNull(annotation);
		assertEquals(HighlightReconcilingStrategy.READ_ANNOTATION_TYPE, annotation.getType());

		annotation = annotations.get(new org.eclipse.jface.text.Position(7, 5));
		Assert.assertNotNull(annotation);
		assertEquals(HighlightReconcilingStrategy.WRITE_ANNOTATION_TYPE, annotation.getType());

		annotation = annotations.get(new org.eclipse.jface.text.Position(13, 4));
		Assert.assertNotNull(annotation);
		assertEquals(HighlightReconcilingStrategy.TEXT_ANNOTATION_TYPE, annotation.getType());

		assertEquals(false, iterator.hasNext());
	}

	@Test
	public void testCheckIfOtherAnnotationsRemains() throws CoreException, InvocationTargetException {
		checkGenericEditorVersion();

		IFile testFile = TestUtils.createUniqueTestFile(project, "  READ WRITE TEXT");
		ITextViewer viewer = TestUtils.openTextViewer(testFile);

		List<DocumentHighlight> highlights = Collections.singletonList(
				new DocumentHighlight(new Range(new Position(0, 2), new Position(0, 6)), DocumentHighlightKind.Read));
		MockLanguageSever.INSTANCE.setDocumentHighlights(highlights);

		if (!(viewer instanceof ISourceViewer)) {
			Assert.fail();
		}

		ISourceViewer sourceViewer = (ISourceViewer) viewer;
		IAnnotationModel model = sourceViewer.getAnnotationModel();

		String fakeAnnotationType = "FAKE_TYPE";
		Annotation fakeAnnotation = new Annotation(fakeAnnotationType, false, null);
		org.eclipse.jface.text.Position fakeAnnotationPosition = new org.eclipse.jface.text.Position(0, 10);
		model.addAnnotation(fakeAnnotation, fakeAnnotationPosition);

		// emulate cursor move
		viewer.getTextWidget().setCaretOffset(1);

		new DisplayHelper() {
			@Override
			protected boolean condition() {
				Iterator<Annotation> iterator = sourceViewer.getAnnotationModel().getAnnotationIterator();
				final AtomicInteger sum = new AtomicInteger(0);
				iterator.forEachRemaining(element -> sum.incrementAndGet());
				return sum.get() == 2;
			}
		}.waitForCondition(Display.getCurrent(), 3000);

		Iterator<Annotation> iterator = model.getAnnotationIterator();
		Map<org.eclipse.jface.text.Position, Annotation> annotations = new HashMap<>();
		while (iterator.hasNext()) {
			Annotation annotation = iterator.next();
			annotations.put(model.getPosition(annotation), annotation);
		}

		Annotation annotation = annotations.get(new org.eclipse.jface.text.Position(2, 4));
		Assert.assertNotNull(annotation);
		assertEquals(HighlightReconcilingStrategy.READ_ANNOTATION_TYPE, annotation.getType());

		annotation = annotations.get(fakeAnnotationPosition);
		Assert.assertNotNull(annotation);
		assertEquals(fakeAnnotationType, annotation.getType());
	}

	private void checkGenericEditorVersion() {
		// ignore tests for generic editor wihtout reconciler API
		Bundle bundle = Platform.getBundle("org.eclipse.ui.genericeditor");
		Assume.assumeTrue(bundle.getVersion().compareTo(new Version(1, 1, 0)) >= 0);
	}

}
