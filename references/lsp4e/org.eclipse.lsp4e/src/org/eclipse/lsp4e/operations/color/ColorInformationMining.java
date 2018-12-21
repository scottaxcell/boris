/**
 *  Copyright (c) 2018 Angelo ZERR.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - [code mining] Support 'textDocument/documentColor' with CodeMining - Bug 533322
 */
package org.eclipse.lsp4e.operations.color;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.codemining.LineContentCodeMining;
import org.eclipse.jface.util.Geometry;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4e.LanguageServerPlugin;
import org.eclipse.lsp4j.ColorInformation;
import org.eclipse.lsp4j.ColorPresentationParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * Draw the LSP color information with a colorized square.
 *
 */
public class ColorInformationMining extends LineContentCodeMining {

	private final RGBA rgba;
	private final DocumentColorProvider colorProvider;

	/**
	 * Click on colorized square opens a color dialog to pick a color and update
	 * text color declaration.
	 *
	 */
	private static class UpdateColorWithDialog implements Consumer<MouseEvent> {

		private final TextDocumentIdentifier textDocumentIdentifier;
		private final ColorInformation colorInformation;
		private final CompletableFuture<LanguageServer> languageServer;
		private final IDocument document;

		public UpdateColorWithDialog(TextDocumentIdentifier textDocumentIdentifier, ColorInformation colorInformation,
				CompletableFuture<LanguageServer> languageServer, IDocument document) {
			this.textDocumentIdentifier = textDocumentIdentifier;
			this.colorInformation = colorInformation;
			this.languageServer = languageServer;
			this.document = document;
		}

		@Override
		public void accept(MouseEvent event) {
			StyledText styledText = (StyledText) event.widget;
			Shell shell = new Shell(styledText.getDisplay());
			Rectangle location = Geometry.toDisplay(styledText, new Rectangle(event.x, event.y, 1, 1));
			shell.setLocation(location.x, location.y);
			// Open color dialog
			ColorDialog dialog = new ColorDialog(shell);
			dialog.setRGB(LSPEclipseUtils.toRGBA(colorInformation.getColor()).rgb);
			RGB rgb = dialog.open();
			if (rgb != null) {
				// get LSP color presentation list for the picked color
				ColorPresentationParams params = new ColorPresentationParams(textDocumentIdentifier,
						LSPEclipseUtils.toColor(rgb), colorInformation.getRange());
				this.languageServer.thenCompose(
						ls -> ls.getTextDocumentService().colorPresentation(params).thenAccept(presentations -> {
							if (presentations.isEmpty()) {
								return;
							}
							// As ColorDialog cannot be customized (to choose the color presentation (rgb,
							// hexa, ....) we pick the first color presentation.
							try {
								TextEdit textEdit = presentations.get(0).getTextEdit();
								LSPEclipseUtils.applyEdit(textEdit, document);
							} catch (BadLocationException e) {
								LanguageServerPlugin.logError(e);
							}
						}));
			}
		}

	}

	public ColorInformationMining(ColorInformation colorInformation, @NonNull IDocument document,
			TextDocumentIdentifier textDocumentIdentifier, CompletableFuture<LanguageServer> languageServer,
			DocumentColorProvider colorProvider) throws BadLocationException {
		super(toPosition(colorInformation.getRange(), document), colorProvider,
				new UpdateColorWithDialog(textDocumentIdentifier, colorInformation, languageServer, document));
		this.rgba = LSPEclipseUtils.toRGBA(colorInformation.getColor());
		this.colorProvider = colorProvider;
		// set label with space to mark the mining as resolved.
		super.setLabel(" "); //$NON-NLS-1$
	}

	@Override
	public Point draw(GC gc, StyledText textWidget, Color color, int x, int y) {
		FontMetrics fontMetrics = gc.getFontMetrics();
		// Compute position and size of the color square
		int size = getSquareSize(fontMetrics);
		x += fontMetrics.getLeading();
		y += fontMetrics.getDescent();
		Rectangle rect = new Rectangle(x, y, size, size);
		// Fill square
		gc.setBackground(colorProvider.getColor(this.rgba, textWidget.getDisplay()));
		gc.fillRectangle(rect);
		// Draw square box
		gc.setForeground(textWidget.getForeground());
		gc.drawRectangle(rect);
		return new Point(getSquareWidth(fontMetrics), size);
	}

	/**
	 * Returns the colorized square size.
	 *
	 * @param fontMetrics
	 * @return the colorized square size.
	 */
	private static int getSquareSize(FontMetrics fontMetrics) {
		return fontMetrics.getHeight() - 2 * fontMetrics.getDescent();
	}

	/**
	 * Compute width of square
	 *
	 * @param styledText
	 * @return the width of square
	 */
	private static int getSquareWidth(FontMetrics fontMetrics) {
		// width = 1 space + size width of square
		int width = (int) fontMetrics.getAverageCharacterWidth() + getSquareSize(fontMetrics);
		return width;
	}

	/**
	 * Returns the Eclipse position from the given LSP range.
	 *
	 * @param range
	 *                     the LSP range to convert
	 * @param document
	 * @return the Eclipse position from the given LSP range.
	 * @throws BadLocationException
	 */
	private static Position toPosition(Range range, IDocument document) throws BadLocationException {
		int start = LSPEclipseUtils.toOffset(range.getStart(), document);
		int end = LSPEclipseUtils.toOffset(range.getEnd(), document);
		return new Position(start, end - start);
	}
}
