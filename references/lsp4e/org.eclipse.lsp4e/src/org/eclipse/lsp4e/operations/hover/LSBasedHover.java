/*******************************************************************************
 * Copyright (c) 2016, 2018 Red Hat Inc. and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Mickael Istria (Red Hat Inc.) - initial implementation
 *  Lucas Bullen (Red Hat Inc.) - Bug 508458 - Add support for codelens
 *  Angelo Zerr <angelo.zerr@gmail.com> - Bug 525602 - LSBasedHover must check if LS have codelens capability
 *  Lucas Bullen (Red Hat Inc.) - [Bug 517428] Requests sent before initialization
 *******************************************************************************/
package org.eclipse.lsp4e.operations.hover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.internal.text.html.BrowserInformationControl;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4e.LanguageServerPlugin;
import org.eclipse.lsp4e.LanguageServiceAccessor;
import org.eclipse.lsp4e.LanguageServiceAccessor.LSPDocumentInfo;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.mylyn.wikitext.markdown.MarkdownLanguage;
import org.eclipse.mylyn.wikitext.parser.MarkupParser;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

/**
 * LSP implementation of {@link org.eclipse.jface.text.ITextHover}
 *
 */
public class LSBasedHover implements ITextHover, ITextHoverExtension {

	private static final MarkupParser MARKDOWN_PARSER = new MarkupParser(new MarkdownLanguage());

	private static final LocationListener HYPER_LINK_LISTENER = new LocationListener() {

		@Override
		public void changing(LocationEvent event) {
			if (!"about:blank".equals(event.location)) { //$NON-NLS-1$
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				LSPEclipseUtils.open(event.location, page, null);
				event.doit = false;
			}
		}

		@Override
		public void changed(LocationEvent event) {
		}
	};

	private static class FocusableBrowserInformationControl extends BrowserInformationControl {

		public FocusableBrowserInformationControl(Shell parent) {
			super(parent, JFaceResources.DEFAULT_FONT, EditorsUI.getTooltipAffordanceString());
		}

		@Override
		protected void createContent(Composite parent) {
			super.createContent(parent);
			Browser b = (Browser) (parent.getChildren()[0]);
			b.addProgressListener(ProgressListener.completedAdapter(event -> {
				if (getInput() == null)
					return;
				Browser browser = (Browser) event.getSource();
				@Nullable
				Point constraints = getSizeConstraints();
				Point hint = computeSizeHint();

				setSize(hint.x, hint.y);
				browser.execute("document.getElementsByTagName(\"html\")[0].style.whiteSpace = \"nowrap\""); //$NON-NLS-1$
				Double width = 20 + (Double) browser.evaluate("return document.body.scrollWidth;"); //$NON-NLS-1$
				if (constraints != null && constraints.x < width) {
					width = (double) constraints.x;
				}

				setSize(width.intValue(), hint.y);
				browser.execute("document.getElementsByTagName(\"html\")[0].style.whiteSpace = \"normal\""); //$NON-NLS-1$
				Double height = (Double) browser.evaluate("return document.body.scrollHeight;"); //$NON-NLS-1$
				if (constraints != null && constraints.y < height) {
					height = (double) constraints.y;
				}
				if (Platform.getPreferencesService().getBoolean(EditorsUI.PLUGIN_ID,
						AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SHOW_TEXT_HOVER_AFFORDANCE, true,
						null)) {
					FontData[] fontDatas = JFaceResources.getDialogFont().getFontData();
					height = fontDatas[0].getHeight() + height;
				}
				setSize(width.intValue(), height.intValue());
			}));
			b.setJavascriptEnabled(true);
		}

		@Override
		public IInformationControlCreator getInformationPresenterControlCreator() {
			return new IInformationControlCreator() {
				@Override
				public IInformationControl createInformationControl(Shell parent) {
					BrowserInformationControl res = new BrowserInformationControl(parent, JFaceResources.DEFAULT_FONT,
							true);
					res.addLocationListener(HYPER_LINK_LISTENER);
					return res;
				}
			};
		}
	}

	private List<CompletableFuture<?>> requests;
	private List<Hover> hoverResults;
	private IRegion lastRegion;
	private ITextViewer textViewer;

	public LSBasedHover() {
	}

	public static String styleHtml(String html) {
		// put CSS styling to match Eclipse style
		ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
		Color foreground = colorRegistry.get("org.eclipse.ui.workbench.HOVER_FOREGROUND"); //$NON-NLS-1$
		Color background = colorRegistry.get("org.eclipse.ui.workbench.HOVER_BACKGROUND"); //$NON-NLS-1$
		String style = "<style TYPE='text/css'>html { " + //$NON-NLS-1$
				"font-family: " + JFaceResources.getDefaultFontDescriptor().getFontData()[0].getName() + "; " + //$NON-NLS-1$ //$NON-NLS-2$
				"font-size: " + Integer.toString(JFaceResources.getDefaultFontDescriptor().getFontData()[0].getHeight()) //$NON-NLS-1$
				+ "pt; " + //$NON-NLS-1$
				(background != null ? "background-color: " + toHTMLrgb(background.getRGB()) + "; " : "") + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				(foreground != null ? "color: " + toHTMLrgb(foreground.getRGB()) + "; " : "") + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				" }</style>"; //$NON-NLS-1$

		int headIndex = html.indexOf("<head>"); //$NON-NLS-1$
		StringBuilder builder = new StringBuilder(html.length() + style.length());
		builder.append(html.substring(0, headIndex + "<head>".length())); //$NON-NLS-1$
		builder.append(style);
		builder.append(html.substring(headIndex + "<head>".length())); //$NON-NLS-1$
		return builder.toString();
	}

	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		if (textViewer == null || hoverRegion == null) {
			return null;
		}
		if (!(hoverRegion.equals(this.lastRegion) && textViewer.equals(this.textViewer) && this.requests != null)) {
			initiateHoverRequest(textViewer, hoverRegion.getOffset());
		}
		try {
			CompletableFuture.allOf(this.requests.toArray(new CompletableFuture[this.requests.size()])).get(500,
					TimeUnit.MILLISECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			LanguageServerPlugin.logError(e);
		}
		String result = ""; //$NON-NLS-1$
		if (!(this.hoverResults == null || this.hoverResults.isEmpty())) {
			result += hoverResults.stream()
				.filter(Objects::nonNull)
				.map(LSBasedHover::getHoverString)
				.filter(Objects::nonNull)
				.collect(Collectors.joining("\n\n")); //$NON-NLS-1$

		}
		if (result.isEmpty()) {
			return null;
		}
		result = MARKDOWN_PARSER.parseToHtml(result);
		return styleHtml(result);
	}

	protected static @Nullable String getHoverString(@NonNull Hover hover) {
		Either<List<Either<String, MarkedString>>, MarkupContent> hoverContent = hover.getContents();
		if (hoverContent.isLeft()) {
			List<Either<String, MarkedString>> contents = hoverContent.getLeft();
			if (contents == null || contents.isEmpty()) {
				return null;
			}
			return contents.stream().map(content -> {
				if (content.isLeft()) {
					return content.getLeft();
				} else if (content.isRight()) {
					MarkedString markedString = content.getRight();
					// TODO this won't work fully until markup parser will support syntax
					// highlighting but will help display
					// strings with language tags, e.g. without it things after <?php tag aren't
					// displayed
					if (markedString.getLanguage() != null && !markedString.getLanguage().isEmpty()) {
						return String.format("```%s\n%s\n```", markedString.getLanguage(), markedString.getValue()); //$NON-NLS-1$
					} else {
						return markedString.getValue();
					}
				} else {
					return ""; //$NON-NLS-1$
				}
			}).filter(((Predicate<String>) String::isEmpty).negate()).collect(Collectors.joining("\n\n")); //$NON-NLS-1$ )
		} else {
			return hoverContent.getRight().getValue();
		}
	}

	private static @NonNull String toHTMLrgb(RGB rgb) {
		StringBuilder builder = new StringBuilder(7);
		builder.append('#');
		appendAsHexString(builder, rgb.red);
		appendAsHexString(builder, rgb.green);
		appendAsHexString(builder, rgb.blue);
		return builder.toString();
	}

	private static void appendAsHexString(StringBuilder buffer, int intValue) {
		String hexValue= Integer.toHexString(intValue);
		if (hexValue.length() == 1) {
			buffer.append('0');
		}
		buffer.append(hexValue);
	}

	@Override
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		if (textViewer == null) {
			return null;
		}
		IRegion res = null;
		initiateHoverRequest(textViewer, offset);
		try {
			CompletableFuture.allOf(this.requests.toArray(new CompletableFuture[this.requests.size()])).get(800, TimeUnit.MILLISECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e1) {
			LanguageServerPlugin.logError(e1);
		}
		final IDocument document = textViewer.getDocument();
		if (!this.hoverResults.isEmpty()) {
			res = new Region(0, document.getLength());
			for (Hover hover : this.hoverResults) {
				int rangeOffset = offset;
				int rangeLength = 0;
				if (hover != null && hover.getRange() != null) {
					try {
						Range range = hover.getRange();
						rangeOffset = LSPEclipseUtils.toOffset(range.getStart(), document);
						rangeLength = LSPEclipseUtils.toOffset(range.getEnd(), document) - rangeOffset;
					} catch (BadLocationException e) {
						LanguageServerPlugin.logError(e);
						res = new Region(offset, 1);
					}
				}
				res = new Region(
						Math.max(res.getOffset(), rangeOffset),
						Math.min(res.getLength(), rangeLength));
			}
		} else {
			res = new Region(offset, 1);
		}

		this.lastRegion = res;
		this.textViewer = textViewer;
		return res;
	}

	/**
	 * Initialize hover requests with hover (if available) and codelens (if
	 * available).
	 *
	 * @param viewer
	 *            the text viewer.
	 * @param offset
	 *            the hovered offset.
	 */
	private void initiateHoverRequest(@NonNull ITextViewer viewer, int offset) {
		this.textViewer = viewer;
		// use intermediary variables to make the lists specific to the request
		// if we directly add/remove from members, we may have thread related issues
		// such as some
		// results from a previous request leaking in the new hover.
		final List<CompletableFuture<?>> requests = new ArrayList<>();
		IDocument document = viewer.getDocument();
		this.hoverResults = getHoverResults(document, offset, requests);
		this.requests = requests;
	}

	/**
	 * Returns the list of hover.
	 *
	 * @param document
	 *            the document of text viewer.
	 * @param offset
	 *            the hovered offset.
	 * @param requests
	 * @return the list of hover.
	 */
	private List<Hover> getHoverResults(@NonNull IDocument document, int offset, List<CompletableFuture<?>> requests) {
		List<@NonNull LSPDocumentInfo> docInfos = LanguageServiceAccessor.getLSPDocumentInfosFor(document,
				(capabilities) -> Boolean.TRUE.equals(capabilities.getHoverProvider()));
		final List<Hover> hoverResults = Collections.synchronizedList(new ArrayList<>(docInfos.size()));
		for (@NonNull
		final LSPDocumentInfo info : docInfos) {
			info.getInitializedLanguageClient().thenAccept(languageServer -> {
				try {
					CompletableFuture<Hover> hover = languageServer.getTextDocumentService().hover(LSPEclipseUtils
							.toTextDocumentPosistionParams(info.getFileUri(), offset, info.getDocument()));
					requests.add(hover.thenAccept(hoverResults::add));
				} catch (BadLocationException e) {
					LanguageServerPlugin.logError(e);
				}
			});
		}
		return hoverResults;
	}

	@Override
	public IInformationControlCreator getHoverControlCreator() {
		return new AbstractReusableInformationControlCreator() {
			@Override
			protected IInformationControl doCreateInformationControl(Shell parent) {
				if (BrowserInformationControl.isAvailable(parent)) {
					return new FocusableBrowserInformationControl(parent);
				} else {
					return new DefaultInformationControl(parent);
				}
			}
		};
	}
}
