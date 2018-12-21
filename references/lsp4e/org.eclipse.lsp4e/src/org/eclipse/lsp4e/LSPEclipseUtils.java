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
 *  Michał Niewrzał (Rogue Wave Software Inc.)
 *  Lucas Bullen (Red Hat Inc.) - Get IDocument from IEditorInput
 *  Angelo Zerr <angelo.zerr@gmail.com> - Bug 525400 - [rename] improve rename support with ltk UI
 *  Remy Suen <remy.suen@gmail.com> - Bug 520052 - Rename assumes that workspace edits are in reverse order
 *  Martin Lippert (Pivotal Inc.) - bug 531452, bug 532305
 *******************************************************************************/
package org.eclipse.lsp4e;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.IFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.RewriteSessionEditProcessor;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.lsp4e.refactoring.CreateFileChange;
import org.eclipse.lsp4j.Color;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.CreateFile;
import org.eclipse.lsp4j.DeleteFile;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.RenameFile;
import org.eclipse.lsp4j.ResourceOperation;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.resource.DeleteResourceChange;
import org.eclipse.mylyn.wikitext.markdown.MarkdownLanguage;
import org.eclipse.mylyn.wikitext.parser.MarkupParser;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.eclipse.text.undo.IDocumentUndoManager;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.intro.config.IIntroURL;
import org.eclipse.ui.intro.config.IntroURLFactory;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditor;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Some utility methods to convert between Eclipse and LS-API types
 */
public class LSPEclipseUtils {

	private static final String HTML = "html"; //$NON-NLS-1$
	private static final String MARKDOWN = "markdown"; //$NON-NLS-1$
	private static final String MD = "md"; //$NON-NLS-1$
	private static final int MAX_BROWSER_NAME_LENGTH = 30;
	private static final MarkupParser MARKDOWN_PARSER = new MarkupParser(new MarkdownLanguage());

	private LSPEclipseUtils() {
		// this class shouldn't be instantiated
	}

	public static ITextEditor getActiveTextEditor() {
		IEditorPart editorPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if(editorPart instanceof ITextEditor) {
			return (ITextEditor) editorPart;
		} else if (editorPart instanceof MultiPageEditorPart) {
			MultiPageEditorPart multiPageEditorPart = (MultiPageEditorPart) editorPart;
			Object page = multiPageEditorPart.getSelectedPage();
			if (page instanceof ITextEditor) {
				return (ITextEditor) page;
			}
		}
		return null;
	}

	public static Position toPosition(int offset, IDocument document) throws BadLocationException {
		Position res = new Position();
		res.setLine(document.getLineOfOffset(offset));
		res.setCharacter(offset - document.getLineInformationOfOffset(offset).getOffset());
		return res;
	}

	public static int toOffset(Position position, IDocument document) throws BadLocationException {
		return document.getLineInformation(position.getLine()).getOffset() + position.getCharacter();
	}

	public static CompletionParams toCompletionParams(URI fileUri, int offset, IDocument document)
			throws BadLocationException {
		Position start = toPosition(offset, document);
		CompletionParams param = new CompletionParams();
		param.setPosition(start);
		param.setUri(fileUri.toString());
		TextDocumentIdentifier id = new TextDocumentIdentifier();
		id.setUri(fileUri.toString());
		param.setTextDocument(id);
		return param;
	}

	public static TextDocumentPositionParams toTextDocumentPosistionParams(URI fileUri, int offset, IDocument document)
			throws BadLocationException {
		Position start = toPosition(offset, document);
		TextDocumentPositionParams param = new TextDocumentPositionParams();
		param.setPosition(start);
		param.setUri(fileUri.toString());
		TextDocumentIdentifier id = new TextDocumentIdentifier();
		id.setUri(fileUri.toString());
		param.setTextDocument(id);
		return param;
	}

	public static int toEclipseMarkerSeverity(DiagnosticSeverity lspSeverity) {
		if (lspSeverity == null) {
			// if severity is empty it is up to the client to interpret diagnostics
			return IMarker.SEVERITY_ERROR;
		}
		switch (lspSeverity) {
		case Error:
			return IMarker.SEVERITY_ERROR;
		case Warning:
			return IMarker.SEVERITY_WARNING;
		default:
			return IMarker.SEVERITY_INFO;
		}
	}

	public static IFile getFileHandle(@Nullable String uri) {
		if (uri == null || uri.isEmpty() || !uri.startsWith("file:")) { //$NON-NLS-1$
			return null;
		}

		String convertedUri = uri.replace("file:///", "file:/"); //$NON-NLS-1$//$NON-NLS-2$
		convertedUri = convertedUri.replace("file://", "file:/"); //$NON-NLS-1$//$NON-NLS-2$
		IPath path = Path.fromOSString(new File(URI.create(convertedUri)).getAbsolutePath());
		IProject project = null;
		for (IProject aProject : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			IPath location = aProject.getLocation();
			if (location != null && location.isPrefixOf(path)
					&& (project == null || project.getLocation().segmentCount() < location.segmentCount())) {
				project = aProject;
			}
		}
		if (project == null) {
			return null;
		}
		IPath projectRelativePath = path.removeFirstSegments(project.getLocation().segmentCount());
		if (projectRelativePath.isEmpty()) {
			return null;
		}
		return project.getFile(projectRelativePath);
	}

	@Nullable
	public static IResource findResourceFor(@Nullable String uri) {
		if (uri == null || uri.isEmpty() || !uri.startsWith("file:")) { //$NON-NLS-1$
			return null;
		}

		String convertedUri = uri.replace("file:///", "file:/"); //$NON-NLS-1$//$NON-NLS-2$
		convertedUri = convertedUri.replace("file://", "file:/"); //$NON-NLS-1$//$NON-NLS-2$
		IPath path = Path.fromOSString(new File(URI.create(convertedUri)).getAbsolutePath());
		IProject project = null;
		for (IProject aProject : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			IPath location = aProject.getLocation();
			if (location != null && location.isPrefixOf(path)
					&& (project == null || project.getLocation().segmentCount() < location.segmentCount())) {
				project = aProject;
			}
		}
		if (project == null) {
			return null;
		}
		IPath projectRelativePath = path.removeFirstSegments(project.getLocation().segmentCount());
		if (projectRelativePath.isEmpty()) {
			return project;
		} else {
			return project.findMember(projectRelativePath);
		}
	}

	public static void applyEdit(TextEdit textEdit, IDocument document) throws BadLocationException {
		document.replace(
				LSPEclipseUtils.toOffset(textEdit.getRange().getStart(), document),
				LSPEclipseUtils.toOffset(textEdit.getRange().getEnd(), document) - LSPEclipseUtils.toOffset(textEdit.getRange().getStart(), document),
				textEdit.getNewText());
	}

	/**
	 * Method will apply all edits to document as single modification. Needs to
	 * be executed in UI thread.
	 *
	 * @param document
	 *            document to modify
	 * @param edits
	 *            list of LSP TextEdits
	 */
	public static void applyEdits(IDocument document, List<? extends TextEdit> edits) {
		if (document == null || edits == null || edits.isEmpty()) {
			return;
		}

		IDocumentUndoManager manager = DocumentUndoManagerRegistry.getDocumentUndoManager(document);
		if (manager != null) {
			manager.beginCompoundChange();
		}

		MultiTextEdit edit = new MultiTextEdit();
		for (TextEdit textEdit : edits) {
			if (textEdit != null) {
				try {
					int offset = LSPEclipseUtils.toOffset(textEdit.getRange().getStart(), document);
					int length = LSPEclipseUtils.toOffset(textEdit.getRange().getEnd(), document) - offset;
					edit.addChild(new ReplaceEdit(offset, length, textEdit.getNewText()));
				} catch (BadLocationException e) {
					LanguageServerPlugin.logError(e);
				}
			}
		}
		try {
			RewriteSessionEditProcessor editProcessor = new RewriteSessionEditProcessor(document, edit,
					org.eclipse.text.edits.TextEdit.NONE);
			editProcessor.performEdits();
		} catch (MalformedTreeException | BadLocationException e) {
			LanguageServerPlugin.logError(e);
		}
		if (manager != null) {
			manager.endCompoundChange();
		}
	}

	@Nullable
	public static IDocument getDocument(@Nullable IResource resource) {
		if (resource == null) {
			return null;
		}

		ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
		IDocument document = null;
		ITextFileBuffer buffer = bufferManager.getTextFileBuffer(resource.getFullPath(), LocationKind.IFILE);
		if (buffer != null) {
			document = buffer.getDocument();
		} else if (resource.getType() == IResource.FILE) {
			try {
				bufferManager.connect(resource.getFullPath(), LocationKind.IFILE, new NullProgressMonitor());
			} catch (CoreException e) {
				LanguageServerPlugin.logError(e);
				return document;
			}
			buffer = bufferManager.getTextFileBuffer(resource.getFullPath(), LocationKind.IFILE);
			if (buffer != null) {
				document = buffer.getDocument();
			}
		}
		return document;
	}

	public static void openInEditor(Location location, IWorkbenchPage page) {
		open(location.getUri(), page, location.getRange());
	}

	public static void open(String uri, IWorkbenchPage page, Range optionalRange) {
		if (uri.startsWith("file:")) { //$NON-NLS-1$
			openFileLocationInEditor(uri, page, optionalRange);
		} else if (uri.startsWith("http://org.eclipse.ui.intro")) { //$NON-NLS-1$
			openIntroURL(uri);
		} else if (uri.startsWith("http")) { //$NON-NLS-1$
			openHttpLocationInBrowser(uri, page);
		}
	}

	protected static void openIntroURL(final String uri) {
		IIntroURL introUrl = IntroURLFactory.createIntroURL(uri);
		if (introUrl != null) {
			try {
				if (!introUrl.execute()) {
					LanguageServerPlugin.logWarning("Failed to execute IntroURL: " + uri, null); // $NON-NLS-1$ //$NON-NLS-1$
				}
			} catch (Throwable t) {
				LanguageServerPlugin.logWarning("Error executing IntroURL: " + uri, t); // $NON-NLS-1$ //$NON-NLS-1$
			}
		}
	}

	protected static void openHttpLocationInBrowser(final String uri, IWorkbenchPage page) {
		page.getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					URL url = new URL(uri);

					IWorkbenchBrowserSupport browserSupport = page.getWorkbenchWindow().getWorkbench()
							.getBrowserSupport();

					String browserName = uri;
					if (browserName.length() > MAX_BROWSER_NAME_LENGTH) {
						browserName = uri.substring(0, MAX_BROWSER_NAME_LENGTH - 1) + '\u2026';
					}

					browserSupport
							.createBrowser(IWorkbenchBrowserSupport.AS_EDITOR | IWorkbenchBrowserSupport.LOCATION_BAR
									| IWorkbenchBrowserSupport.NAVIGATION_BAR, "lsp4e-symbols", browserName, uri) //$NON-NLS-1$
							.openURL(url);

				} catch (Exception e) {
					LanguageServerPlugin.logError(e);
				}
			}
		});
	}

	protected static void openFileLocationInEditor(String uri, IWorkbenchPage page, Range optionalRange) {
		IEditorPart part = null;
		IDocument targetDocument = null;
		IResource targetResource = LSPEclipseUtils.findResourceFor(uri);
		try {
			if (targetResource != null && targetResource.getType() == IResource.FILE) {
				part = IDE.openEditor(page, (IFile) targetResource);
				targetDocument = FileBuffers.getTextFileBufferManager()
				        .getTextFileBuffer(targetResource.getFullPath(), LocationKind.IFILE).getDocument();
			} else {
				URI fileUri = URI.create(uri).normalize();
				IFileStore fileStore =  EFS.getLocalFileSystem().getStore(fileUri);
				IFileInfo fetchInfo = fileStore.fetchInfo();
				if (!fetchInfo.isDirectory() && fetchInfo.exists()) {
					part = IDE.openEditorOnFileStore(page, fileStore);
					ITextFileBuffer fileStoreTextFileBuffer = FileBuffers.getTextFileBufferManager()
							.getFileStoreTextFileBuffer(fileStore);
					targetDocument = fileStoreTextFileBuffer.getDocument();
				}
			}
		} catch (PartInitException e) {
			LanguageServerPlugin.logError(e);
		}
		try {
			if (part instanceof AbstractTextEditor && optionalRange != null) {
				AbstractTextEditor editor = (AbstractTextEditor) part;
				int offset = LSPEclipseUtils.toOffset(optionalRange.getStart(), targetDocument);
				int endOffset = LSPEclipseUtils.toOffset(optionalRange.getEnd(), targetDocument);
				editor.getSelectionProvider()
				        .setSelection(new TextSelection(offset, endOffset > offset ? endOffset - offset : 0));
			}
		} catch (BadLocationException e) {
			LanguageServerPlugin.logError(e);
		}
	}

	public static IDocument getDocument(ITextEditor editor) {
		try {
			Method getSourceViewerMethod= AbstractTextEditor.class.getDeclaredMethod("getSourceViewer"); //$NON-NLS-1$
			getSourceViewerMethod.setAccessible(true);
			ITextViewer viewer = (ITextViewer) getSourceViewerMethod.invoke(editor);
			return viewer.getDocument();
		} catch (Exception ex) {
			LanguageServerPlugin.logError(ex);
			return null;
		}
	}

	public static IDocument getDocument(IEditorInput editorInput) {
		if(editorInput instanceof IFileEditorInput) {
			IFileEditorInput fileEditorInput = (IFileEditorInput)editorInput;
				return getDocument(fileEditorInput.getFile());
		}else if(editorInput instanceof IPathEditorInput) {
			IPathEditorInput pathEditorInput = (IPathEditorInput)editorInput;
			return getDocument(ResourcesPlugin.getWorkspace().getRoot().getFile(pathEditorInput.getPath()));
		}else if(editorInput instanceof IURIEditorInput) {
			IURIEditorInput uriEditorInput = (IURIEditorInput)editorInput;
			return getDocument(findResourceFor(uriEditorInput.getURI().toString()));
		}
		return null;
	}

	/**
	 * Applies a workspace edit. It does simply change the underlying documents.
	 *
	 * @param wsEdit
	 */
	public static void applyWorkspaceEdit(WorkspaceEdit wsEdit) {
		CompositeChange change = toCompositeChange(wsEdit);
		PerformChangeOperation changeOperation = new PerformChangeOperation(change);
		try {
			ResourcesPlugin.getWorkspace().run(changeOperation, new NullProgressMonitor());
		} catch (CoreException e) {
			LanguageServerPlugin.logError(e);
		}
	}

	/**
	 * Returns a ltk {@link CompositeChange} from a lsp {@link WorkspaceEdit}.
	 *
	 * @param wsEdit
	 * @return a ltk {@link CompositeChange} from a lsp {@link WorkspaceEdit}.
	 */
	public static CompositeChange toCompositeChange(WorkspaceEdit wsEdit) {
		CompositeChange change = new CompositeChange("LSP Workspace Edit"); //$NON-NLS-1$
		List<Either<TextDocumentEdit, ResourceOperation>> documentChanges = wsEdit.getDocumentChanges();
		if (documentChanges != null) {
			// documentChanges are present, the latter are preferred over changes
			// see specification at
			// https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#workspaceedit
			documentChanges.stream().forEach(action -> {
				if (action.isLeft()) {
					TextDocumentEdit edit = action.getLeft();
					VersionedTextDocumentIdentifier id = edit.getTextDocument();
					String uri = id.getUri();
					List<TextEdit> textEdits = edit.getEdits();
					fillTextEdits(uri, textEdits, change);
				} else if (action.isRight()) {
					ResourceOperation resourceOperation = action.getRight();
					if (resourceOperation instanceof CreateFile) {
						CreateFile createOperation = (CreateFile) resourceOperation;
						IFile targetFile = LSPEclipseUtils.getFileHandle(createOperation.getUri());
						if (targetFile == null) {
							// TODO log
							return;
						}
						if (targetFile.exists()) {
							if (createOperation.getOptions().getOverwrite() || !createOperation.getOptions().getIgnoreIfExists()) {
								final IDocument document = LSPEclipseUtils
										.getDocument(LSPEclipseUtils.findResourceFor(createOperation.getUri()));
								if (document != null) {
									try {
										TextEdit edit = new TextEdit(
												new Range(new Position(0, 0),
														LSPEclipseUtils.toPosition(document.getLength() - 1, document)),
												""); //$NON-NLS-1$
										fillTextEdits(createOperation.getUri(), Collections.singletonList(edit),
												change);
									} catch (BadLocationException e) {
										LanguageServerPlugin.logError(e);
									}
								}
							} else {
								return;
							}
						} else {
							CreateFileChange operation = new CreateFileChange(targetFile.getFullPath(), "", null); //$NON-NLS-1$
							change.add(operation);
						}
					} else if (resourceOperation instanceof DeleteFile) {
						IResource resource = LSPEclipseUtils.findResourceFor(((DeleteFile) resourceOperation).getUri());
						if (resource != null) {
							DeleteResourceChange deleteChange = new DeleteResourceChange(resource.getFullPath(), true);
							change.add(deleteChange);
						} else {
							LanguageServerPlugin.logWarning(
									"Changes outside of visible projects are not supported at the moment.", null); //$NON-NLS-1$
						}
					} else if (resourceOperation instanceof RenameFile) {
						URI oldURI = URI.create(((RenameFile) resourceOperation).getOldUri());
						URI newURI = URI.create(((RenameFile) resourceOperation).getNewUri());
						IFile oldFile = LSPEclipseUtils.getFileHandle(oldURI.toString());
						IFile newFile = LSPEclipseUtils.getFileHandle(newURI.toString());
						if (oldFile != null) {
							if (newFile == null) {
								LanguageServerPlugin.logWarning(
										"target file " + newURI.toString() + " cannot be created in workspace.", null); //$NON-NLS-1$ //$NON-NLS-2$
								return;
							}
							DeleteResourceChange removeNewFile = null;
							if (newFile.exists()) {
								if (((RenameFile) resourceOperation).getOptions().getOverwrite()) {
									removeNewFile = new DeleteResourceChange(newFile.getFullPath(),
											true);
								} else if (((RenameFile) resourceOperation).getOptions().getIgnoreIfExists()) {
									return;
								}
							}
							try (ByteArrayOutputStream stream = new ByteArrayOutputStream(
									(int) oldFile.getLocation().toFile().length());
									InputStream inputStream = oldFile.getContents();) {
								FileUtil.transferStreams(inputStream, stream, newFile.getLocation().toString(), null);
								// inputStream.transferTo(stream);
								CreateFileChange createFileChange = new CreateFileChange(newFile.getFullPath(),
										new String(stream.toByteArray()), oldFile.getCharset());
								DeleteResourceChange removeOldFile = new DeleteResourceChange(oldFile.getFullPath(),
										true);
								if (removeNewFile != null) {
									change.add(removeNewFile);
								}
								change.add(createFileChange);
								change.add(removeOldFile);
							} catch (Exception ex) {
								LanguageServerPlugin.logError(ex);
							}
						} else {
							LanguageServerPlugin.logWarning(
									"Source file " + oldURI.toString() + " is missing.", null); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
				}
			});
		} else {
			Map<String, List<TextEdit>> changes = wsEdit.getChanges();
			if (changes != null) {
				for (java.util.Map.Entry<String, List<TextEdit>> edit : changes.entrySet()) {
					String uri = edit.getKey();
					List<TextEdit> textEdits = edit.getValue();
					fillTextEdits(uri, textEdits, change);
				}
			}
		}
		return change;
	}

	/**
	 * Transform LSP {@link TextEdit} list into ltk {@link DocumentChange} and add
	 * it in the given ltk {@link CompositeChange}.
	 *
	 * @param uri
	 *            document URI to update
	 * @param textEdits
	 *            LSP text edits
	 * @param change
	 *            ltk change to update
	 */
	private static void fillTextEdits(String uri, List<TextEdit> textEdits, CompositeChange change) {
		IFile file = LSPEclipseUtils.getFileHandle(uri);
		if (!file.exists()) {
			throw new IllegalArgumentException("Expected existing file."); //$NON-NLS-1$
		}
		IDocument document = null;
		IFileBuffer buffer = FileBuffers.getTextFileBufferManager().getFileBuffer(file.getFullPath(),
				LocationKind.IFILE);
		document = getDocument(file);
		// sort the edits so that the ones at the bottom of the document are first
		// so that they can be applied from bottom to top
		Collections.sort(textEdits, Comparator.comparing(edit -> edit.getRange().getStart(),
				Comparator.comparingInt(Position::getLine).thenComparingInt(Position::getCharacter).reversed()));
		for (TextEdit textEdit : textEdits) {
			try {
				int offset = LSPEclipseUtils.toOffset(textEdit.getRange().getStart(), document);
				int length = LSPEclipseUtils.toOffset(textEdit.getRange().getEnd(), document) - offset;
				TextChange textChange = null;
				if (buffer != null) {
					textChange = new DocumentChange("Change in document " + uri, document); //$NON-NLS-1$
				} else {
					textChange = new TextFileChange("Change in file " + file.getName(), file); //$NON-NLS-1$
				}
				textChange.initializeValidationData(new NullProgressMonitor());
				textChange.setEdit(new ReplaceEdit(offset, length, textEdit.getNewText()));
				change.add(textChange);
			} catch (BadLocationException e) {
				LanguageServerPlugin.logError(e);
			}
		}
	}

	public static URI toUri(IPath absolutePath) {
		return toUri(absolutePath.toFile());
	}

	public static URI toUri(IResource resource) {
		return toUri(resource.getLocation());
	}

	public static URI toUri(File file) {
		// URI scheme specified by language server protocol and LSP
		try {
			return new URI("file", "", file.getAbsoluteFile().toURI().getPath(), null); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (URISyntaxException e) {
			LanguageServerPlugin.logError(e);
			return file.getAbsoluteFile().toURI();
		}
	}

	// TODO consider using Entry/SimpleEntry instead
	private static final class Pair<K, V> {
		K key;
		V value;
		Pair(K key,V value) {
			this.key = key;
			this.value = value;
		}
	}

	/**
	 * Very empirical and unsafe heuristic to turn unknown command arguments
	 * into a workspace edit...
	 */
	public static WorkspaceEdit createWorkspaceEdit(List<Object> commandArguments, IResource initialResource) {
		WorkspaceEdit res = new WorkspaceEdit();
		Map<String, List<TextEdit>> changes = new HashMap<>();
		res.setChanges(changes);
		Pair<IResource, List<TextEdit>> currentEntry = new Pair<>(initialResource, new ArrayList<>());
		commandArguments.stream().flatMap(item -> {
			if (item instanceof List) {
				return ((List<?>)item).stream();
			} else {
				return Collections.singleton(item).stream();
			}
		}).forEach(arg -> {
			if (arg instanceof String) {
				changes.put(currentEntry.key.getLocationURI().toString(), currentEntry.value);
				IResource resource = LSPEclipseUtils.findResourceFor((String)arg);
				if (resource != null) {
					currentEntry.key = resource;
					currentEntry.value = new ArrayList<>();
				}
			} else if (arg instanceof WorkspaceEdit) {
				changes.putAll(((WorkspaceEdit)arg).getChanges());
			} else if (arg instanceof TextEdit) {
				currentEntry.value.add((TextEdit)arg);
			} else if (arg instanceof Map) {
				Gson gson = new Gson(); // TODO? retrieve the GSon used by LS
				TextEdit edit = gson.fromJson(gson.toJson(arg), TextEdit.class);
				if (edit != null) {
					currentEntry.value.add(edit);
				}
			} else if (arg instanceof JsonPrimitive) {
				JsonPrimitive json = (JsonPrimitive) arg;
				if (json.isString()) {
					changes.put(currentEntry.key.getLocationURI().toString(), currentEntry.value);
					IResource resource = LSPEclipseUtils.findResourceFor(json.getAsString());
					if (resource != null) {
						currentEntry.key = resource;
						currentEntry.value = new ArrayList<>();
					}
				}
			} else if (arg instanceof JsonArray) {
				Gson gson = new Gson(); // TODO? retrieve the GSon used by LS
				JsonArray array = (JsonArray) arg;
				array.forEach(elt -> {
					TextEdit edit = gson.fromJson(gson.toJson(elt), TextEdit.class);
					if (edit != null) {
						currentEntry.value.add(edit);
					}
				});
			} else if (arg instanceof JsonObject) {
				Gson gson = new Gson(); // TODO? retrieve the GSon used by LS
				WorkspaceEdit wEdit = gson.fromJson((JsonObject) arg, WorkspaceEdit.class);
				Map<String, List<TextEdit>> entries = wEdit.getChanges();
				if (wEdit != null && !entries.isEmpty()) {
					changes.putAll(entries);
				} else {
					TextEdit edit = gson.fromJson((JsonObject) arg, TextEdit.class);
					if (edit != null && edit.getRange() != null) {
						currentEntry.value.add(edit);
					}
				}
			}
		});
		if (!currentEntry.value.isEmpty()) {
			changes.put(currentEntry.key.getLocationURI().toString(), currentEntry.value);
		}
		return res;
	}

	@Nullable public static IFile getFile(IDocument document) {
		ITextFileBuffer buffer = FileBuffers.getTextFileBufferManager().getTextFileBuffer(document);
		if (buffer == null) {
			return null;
		}
		final IPath location = buffer.getLocation();
		return ResourcesPlugin.getWorkspace().getRoot().getFile(location);
	}

	@NonNull
	public static WorkspaceFolder toWorkspaceFolder(@NonNull IProject project) {
		WorkspaceFolder folder = new WorkspaceFolder();
		folder.setUri(project.getLocationURI().toString());
		folder.setName(project.getName());
		return folder;
	}

	@NonNull
	public static List<IContentType> getFileContentTypes(@NonNull IFile file) {
		List<IContentType> contentTypes = new ArrayList<>();
		try (InputStream contents = file.getContents()) {
			// TODO consider using document as inputstream
			contentTypes.addAll(
					Arrays.asList(Platform.getContentTypeManager().findContentTypesFor(contents, file.getName())));
		} catch (CoreException | IOException e) {
			LanguageServerPlugin.logError(e);
		}
		return contentTypes;
	}

	/**
	 * Deprecated because any code that calls this probably needs to be changed
	 * somehow to be properly aware of markdown content. This method simply returns
	 * the doc string as a string, regardless of whether it is markdown or
	 * plaintext.
	 */
	@Deprecated
	public static String getDocString(Either<String, MarkupContent> documentation) {
		if (documentation != null) {
			if (documentation.isLeft()) {
				return documentation.getLeft();
			} else {
				return documentation.getRight().getValue();
			}
		}
		return null;
	}

	public static String getHtmlDocString(Either<String, MarkupContent> documentation) {
		if (documentation.isLeft()) {
			return htmlParagraph(documentation.getLeft());
		} else if (documentation.isRight()) {
			MarkupContent markupContent = documentation.getRight();
			if (markupContent.getValue() != null) {
				if (MARKDOWN.equalsIgnoreCase(markupContent.getKind())
						|| MD.equalsIgnoreCase(markupContent.getKind())) {
					try {
						return MARKDOWN_PARSER.parseToHtml(markupContent.getValue());
					} catch (Exception e) {
						LanguageServerPlugin.logError(e);
						return htmlParagraph(markupContent.getValue());
					}
				} else if (HTML.equalsIgnoreCase(markupContent.getKind())) {
					return markupContent.getValue();
				} else {
					return htmlParagraph(markupContent.getValue());
				}
			}
		}
		return null;
	}

	private static String htmlParagraph(String text) {
		StringBuilder sb = new StringBuilder();
		sb.append("<p>"); //$NON-NLS-1$
		sb.append(text);
		sb.append("</p>"); //$NON-NLS-1$
		return sb.toString();
	}

	/**
	 * Convert the given Eclipse <code>rgb</code> instance to a LSP {@link Color}
	 * instance.
	 *
	 * @param rgb
	 *            the rgb instance to convert
	 * @return the given Eclipse <code>rgb</code> instance to a LSP {@link Color}
	 *         instance.
	 */
	public static Color toColor(RGB rgb) {
		return new Color(rgb.red / 255d, rgb.green / 255d, rgb.blue / 255d, 1);
	}

	/**
	 * Convert the given LSP <code>color</code> instance to a Eclipse {@link RGBA}
	 * instance.
	 *
	 * @param rgb
	 *            the color instance to convert
	 * @return the given LSP <code>color</code> instance to a Eclipse {@link RGBA}
	 *         instance.
	 */
	public static RGBA toRGBA(Color color) {
		return new RGBA((int) (color.getRed() * 255), (int) (color.getGreen() * 255), (int) (color.getBlue() * 255),
				(int) color.getAlpha());
	}
}
