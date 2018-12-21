/*******************************************************************************
 * Copyright (c) 2017 Kichwa Coders Ltd. and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.lsp4e.debug.debugmodel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.lsp4e.debug.DSPPlugin;
import org.eclipse.lsp4e.debug.console.DSPProcess;
import org.eclipse.lsp4e.debug.console.DSPStreamsProxy;
import org.eclipse.lsp4j.debug.BreakpointEventArguments;
import org.eclipse.lsp4j.debug.Capabilities;
import org.eclipse.lsp4j.debug.ConfigurationDoneArguments;
import org.eclipse.lsp4j.debug.ContinuedEventArguments;
import org.eclipse.lsp4j.debug.DisconnectArguments;
import org.eclipse.lsp4j.debug.ExitedEventArguments;
import org.eclipse.lsp4j.debug.InitializeRequestArguments;
import org.eclipse.lsp4j.debug.LoadedSourceEventArguments;
import org.eclipse.lsp4j.debug.ModuleEventArguments;
import org.eclipse.lsp4j.debug.OutputEventArguments;
import org.eclipse.lsp4j.debug.OutputEventArgumentsCategory;
import org.eclipse.lsp4j.debug.ProcessEventArguments;
import org.eclipse.lsp4j.debug.RunInTerminalRequestArguments;
import org.eclipse.lsp4j.debug.RunInTerminalRequestArgumentsKind;
import org.eclipse.lsp4j.debug.RunInTerminalResponse;
import org.eclipse.lsp4j.debug.StoppedEventArguments;
import org.eclipse.lsp4j.debug.TerminatedEventArguments;
import org.eclipse.lsp4j.debug.Thread;
import org.eclipse.lsp4j.debug.ThreadEventArguments;
import org.eclipse.lsp4j.debug.ThreadsResponse;
import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.validation.ReflectiveMessageValidator;

public class DSPDebugTarget extends DSPDebugElement implements IDebugTarget, IDebugProtocolClient {
	private static final boolean TRACE_IO = Boolean
			.parseBoolean(Platform.getDebugOption("org.eclipse.lsp4e.debug/trace/io")); //$NON-NLS-1$
	private static final boolean TRACE_MESSAGES = Boolean
			.parseBoolean(Platform.getDebugOption("org.eclipse.lsp4e.debug/trace/messages")); //$NON-NLS-1$

	/**
	 * Any events we receive from the adapter that require further contact with the
	 * adapter needs to be farmed off to another thread as the events arrive on the
	 * same thread. (Note for requests, use the *Async versions on
	 * completeablefuture to achieve the same effect.)
	 */
	private ExecutorService threadPool = Executors.newCachedThreadPool();

	private ILaunch launch;
	private Future<?> debugProtocolFuture;
	private IDebugProtocolServer debugProtocolServer;
	private Capabilities capabilities;
	/**
	 * Once we have received initialized event, this member will be "done" as a flag
	 */
	private CompletableFuture<Void> initialized = new CompletableFuture<Void>();

	/**
	 * The cached set of current threads. This should generally not be directly
	 * accessed and instead accessed via {@link #getThreads()} which will ensure
	 * they are up to date (against the {@link #refreshThreads} flag).
	 */
	private Map<Long, DSPThread> threads = Collections.synchronizedMap(new TreeMap<>());
	/**
	 * Set to true to update the threads list from the debug adapter.
	 */
	private AtomicBoolean refreshThreads = new AtomicBoolean(true);

	private boolean fTerminated = false;
	private String targetName = null;

	private Runnable processCleanup;
	private DSPBreakpointManager breakpointManager;
	private DSPProcess process;
	private InputStream in;
	private OutputStream out;

	/**
	 * User supplied debug paramters for {@link IDebugProtocolServer#launch(Map)}
	 * and {@link IDebugProtocolServer#attach(Map)}
	 */
	private Map<String, Object> dspParameters;

	public DSPDebugTarget(ILaunch launch, Runnable processCleanup, InputStream in, OutputStream out,
			Map<String, Object> dspParameters) throws CoreException {
		super(null);
		this.in = in;
		this.out = out;
		this.launch = launch;
		this.processCleanup = processCleanup;
		this.dspParameters = dspParameters;
	}

	public void initialize(IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
		try {

			PrintWriter traceMessages;
			if (TRACE_MESSAGES) {
				traceMessages = new PrintWriter(System.out);
			} else {
				traceMessages = null;
			}
			if (TRACE_IO) {
				in = new TraceInputStream(in, System.out);
				out = new TraceOutputStream(out, System.out);
			}
			// TODO this Function copied from createClientLauncher so that I can replace
			// threadpool, so make this wrapper more accessible
			Function<MessageConsumer, MessageConsumer> wrapper = consumer -> {
				MessageConsumer result = consumer;
				if (traceMessages != null) {
					result = message -> {
						traceMessages.println(message);
						traceMessages.flush();
						consumer.consume(message);
					};
				}
				if (true) {
					result = new ReflectiveMessageValidator(result);
				}
				return result;
			};

			Launcher<IDebugProtocolServer> debugProtocolLauncher = DSPLauncher.createClientLauncher(this, in, out,
					threadPool, wrapper);

			debugProtocolFuture = debugProtocolLauncher.startListening();
			debugProtocolServer = debugProtocolLauncher.getRemoteProxy();

			CompletableFuture<Void> future = initialize(dspParameters, subMonitor);
			monitorGet(future, subMonitor);
		} catch (Exception e) {
			terminated();
			throw e;
		} finally {
			subMonitor.done();
		}
	}

	private CompletableFuture<Void> initialize(Map<String, Object> dspParameters, IProgressMonitor monitor) {
		InitializeRequestArguments arguments = new InitializeRequestArguments();
		arguments.setClientID("lsp4e.debug");
		String adapterId = "adapterId";
		if (dspParameters.containsKey("type") && dspParameters.get("type") instanceof String) {
			adapterId = (String)dspParameters.get("type");
		}
		arguments.setAdapterID(adapterId);
		arguments.setPathFormat("path");
		arguments.setSupportsVariableType(true);
		arguments.setSupportsVariablePaging(true);
		arguments.setLinesStartAt1(true);
		arguments.setColumnsStartAt1(true);
		arguments.setSupportsRunInTerminalRequest(true);
		targetName = Objects.toString(dspParameters.get("program"), "Debug Adapter Target");

		monitor.subTask("Initializing connection to debug adapter");
		CompletableFuture<Void> future = getDebugProtocolServer().initialize(arguments)
				.thenAccept((Capabilities capabilities) -> {
					monitor.worked(10);
					this.capabilities = capabilities;
				}).thenRun(() -> {
					process = new DSPProcess(this);
					launch.addProcess(process);
				}).thenCompose((v) -> {
					monitor.worked(10);
					if ("launch".equals(dspParameters.getOrDefault("request", "launch"))) {
						monitor.subTask("Launching program");
						return getDebugProtocolServer().launch(dspParameters);
					} else {
						monitor.subTask("Attaching to running program");
						return getDebugProtocolServer().attach(dspParameters);
					}
				}).thenCombineAsync(initialized, (v1, v2) -> {
					monitor.worked(10);
					return (Void) null;
				}).thenCompose((v) -> {
					monitor.worked(10);
					monitor.subTask("Sending breakpoints");
					breakpointManager = new DSPBreakpointManager(getBreakpointManager(), getDebugProtocolServer(),
							capabilities);
					return breakpointManager.initialize();
				}).thenCompose((v) -> {
					monitor.worked(30);
					monitor.subTask("Sending configuration done");
					if (Boolean.TRUE.equals(capabilities.getSupportsConfigurationDoneRequest())) {
						return getDebugProtocolServer().configurationDone(new ConfigurationDoneArguments());
					}
					return CompletableFuture.completedFuture(null);
				});
		return future;
	}

	private void terminated() {
		fTerminated = true;
		fireTerminateEvent();
		if (breakpointManager != null) {
			breakpointManager.shutdown();
		}
		debugProtocolFuture.cancel(true);
		try {
			in.close();
		} catch (IOException e1) {
			// ignore inner resource exception
		}
		try {
			out.close();
		} catch (IOException e1) {
			// ignore inner resource exception
		}
		processCleanup.run();
	}

	@Override
	public void initialized() {
		initialized.complete(null);
	}

	/**
	 * Throws a debug exception with a status code of
	 * <code>TARGET_REQUEST_FAILED</code>.
	 *
	 * @param message
	 *            exception message
	 * @param e
	 *            underlying exception or <code>null</code>
	 * @throws DebugException
	 *             if a problem is encountered
	 */
	@Override
	protected void requestFailed(String message, Throwable e) throws DebugException {
		throw newTargetRequestFailedException(message, e);
	}

	@Override
	public DSPDebugTarget getDebugTarget() {
		return this;
	}

	@Override
	public ILaunch getLaunch() {
		return launch;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
	public boolean canTerminate() {
		return true;
	}

	@Override
	public boolean isTerminated() {
		return fTerminated;
	}

	@Override
	public void terminated(TerminatedEventArguments body) {
		terminated();
	}

	@Override
	public void terminate() throws DebugException {
		DisconnectArguments arguments = new DisconnectArguments();
		arguments.setTerminateDebuggee(true);
		getDebugProtocolServer().disconnect(arguments).thenRun(this::terminated);
	}

	@Override
	public void continued(ContinuedEventArguments body) {
		threadPool.execute(() -> {
			DSPDebugElement source = null;
			if (body.getThreadId() != null) {
				source = getThread(body.getThreadId());
			}
			if (source == null || body.getAllThreadsContinued() == null || body.getAllThreadsContinued()) {
				Arrays.asList(getThreads()).forEach(t -> t.continued());
			}
			if (source != null) {
				source.fireResumeEvent(DebugEvent.CLIENT_REQUEST);
			}
		});
	}

	@Override
	public void stopped(StoppedEventArguments body) {
		threadPool.execute(() -> {
			DSPDebugElement source = null;
			if (body.getThreadId() != null) {
				source = getThread(body.getThreadId());
			}
			if (source == null || body.getAllThreadsStopped() == null || body.getAllThreadsStopped()) {
				Arrays.asList(getThreads()).forEach(t -> {
					t.stopped();
					t.fireChangeEvent(DebugEvent.CHANGE);
				});
			}

			if (source != null) {
				source.fireSuspendEvent(calcDetail(body.getReason()));
			}
		});
	}

	private int calcDetail(String reason) {
		if (reason.equals("breakpoint")) { //$NON-NLS-1$
			return DebugEvent.BREAKPOINT;
		} else if (reason.equals("step")) { //$NON-NLS-1$
			return DebugEvent.STEP_OVER;
		} else if (reason.equals("pause")) { //$NON-NLS-1$
			return DebugEvent.CLIENT_REQUEST;
		} else {
			return DebugEvent.UNSPECIFIED;
		}
	}

	@Override
	public boolean canResume() {
		return !isTerminated() && isSuspended() && getThreads().length > 0;
	}

	@Override
	public boolean canSuspend() {
		return !isTerminated() && !isSuspended() && getThreads().length > 0;
	}

	@Override
	public boolean isSuspended() {
		DSPThread[] dspThreads = getThreads();
		boolean anyMatch = Arrays.asList(dspThreads).stream().anyMatch(DSPThread::isSuspended);
		return anyMatch;
	}

	@Override
	public void resume() throws DebugException {
		DSPThread[] dspThreads = getThreads();
		// TODO add support to protocol for ContinueArguments to support all
		if (dspThreads.length > 0) {
			dspThreads[0].resume();
		}
	}

	@Override
	public void suspend() throws DebugException {
		DSPThread[] dspThreads = getThreads();
		// TODO add support to protocol for PauseArguments to support all
		if (dspThreads.length > 0) {
			dspThreads[0].suspend();
		}
	}

	@Override
	public boolean canDisconnect() {
		// TODO
		return false;
	}

	@Override
	public void disconnect() throws DebugException {
		// TODO
	}

	@Override
	public boolean isDisconnected() {
		// TODO
		return false;
	}

	@Override
	public boolean supportsStorageRetrieval() {
		return false;
	}

	@Override
	public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException {
		return null;
	}

	@Override
	public IProcess getProcess() {
		return process;
	}

	@Override
	public DSPThread[] getThreads() {
		if (!refreshThreads.getAndSet(false)) {
			synchronized (threads) {
				Collection<DSPThread> values = threads.values();
				return values.toArray(new DSPThread[values.size()]);
			}
		}
		try {
			CompletableFuture<ThreadsResponse> threads2 = getDebugProtocolServer().threads();
			CompletableFuture<DSPThread[]> future = threads2.thenApplyAsync(threadsResponse -> {
				synchronized (threads) {
					Map<Long, DSPThread> lastThreads = new TreeMap<>(threads);
					threads.clear();
					Thread[] body = threadsResponse.getThreads();
					for (Thread thread : body) {
						DSPThread dspThread = lastThreads.get(thread.getId());
						if (dspThread == null) {
							dspThread = new DSPThread(this, thread.getId());
						}
						dspThread.update(thread);
						threads.put(thread.getId(), dspThread);
						// fireChangeEvent(DebugEvent.CONTENT);
					}
					Collection<DSPThread> values = threads.values();
					return values.toArray(new DSPThread[values.size()]);
				}
			});
			return future.get();
		} catch (RuntimeException | ExecutionException e) {
			if (isTerminated()) {
				return new DSPThread[0];
			}
			DSPPlugin.logError(e);
			return new DSPThread[0];
		} catch (InterruptedException e) {
			java.lang.Thread.interrupted();
			return new DSPThread[0];
		}
	}

	/**
	 * Get a thread object without connecting to the debug adapter. This is for when
	 * we get a thread before it is fully populated, so return a new thread in that
	 * case and let it be populated later
	 *
	 * @param threadId
	 * @return
	 */
	private DSPThread getThread(Long threadId) {
		return threads.computeIfAbsent(threadId, id -> new DSPThread(this, threadId));
	}

	@Override
	public boolean hasThreads() throws DebugException {
		return getThreads().length > 0;
	}

	@Override
	public String getName() {
		return targetName;
	}

	@Override
	public boolean supportsBreakpoint(IBreakpoint breakpoint) {
		return !isTerminated() && breakpointManager.supportsBreakpoint(breakpoint);
	}

	@Override
	public void exited(ExitedEventArguments args) {
		// TODO
	}

	@Override
	public void thread(ThreadEventArguments args) {
		refreshThreads.set(true);
		fireChangeEvent(DebugEvent.CONTENT);
	}

	@Override
	public void output(OutputEventArguments args) {
		boolean outputted = false;
		if (process != null) {
			DSPStreamsProxy dspStreamsProxy = process.getStreamsProxy();
			String output = args.getOutput();
			if (args.getCategory() == null || OutputEventArgumentsCategory.CONSOLE.equals(args.getCategory())) {
				// TODO put this data in a different region with a different colour
				dspStreamsProxy.getOutputStreamMonitor().append(output);
				outputted = true;
			} else if (OutputEventArgumentsCategory.STDOUT.equals(args.getCategory())) {
				dspStreamsProxy.getOutputStreamMonitor().append(output);
				outputted = true;
			} else if (OutputEventArgumentsCategory.STDERR.equals(args.getCategory())) {
				dspStreamsProxy.getErrorStreamMonitor().append(output);
				outputted = true;
			}
		}
		if (!outputted && DSPPlugin.DEBUG) {
			System.out.println("output: " + args);
		}

	}

	@Override
	public void module(ModuleEventArguments args) {
		// TODO

	}

	@Override
	public void loadedSource(LoadedSourceEventArguments args) {
		// TODO

	}

	@Override
	public void process(ProcessEventArguments args) {
		// TODO

	}

	/**
	 * runInTerminal request; value of command field is 'runInTerminal'.
	 * <p>
	 * With this request a debug adapter can run a command in a terminal.
	 */
	@JsonRequest
	public CompletableFuture<RunInTerminalResponse> runInTerminal(RunInTerminalRequestArguments args) {
		if (RunInTerminalRequestArgumentsKind.EXTERNAL.equals(args.getKind())) {
			// TODO handle external run in terminal in an external way?
		}
		// TODO use a "real" terminal (like the one in TMF?) for
		// RunInTerminalRequestArgumentsKind.INTEGRATED terminal
		StringBuilder cmd = new StringBuilder();
		for (String arg : args.getArgs()) {
			if (arg.contains(" ")) {
				cmd.append("\"");
				cmd.append(arg);
				cmd.append("\"");
			} else {
				cmd.append(arg);
			}
			cmd.append(" ");
		}
		if (cmd.length() > 0) {
			cmd.setLength(cmd.length() - 1);
		}
		ProcessBuilder processBuilder = new ProcessBuilder(args.getArgs());
		// processBuilder.inheritIO();
		if (args.getCwd() != null) {
			processBuilder.directory(new File(args.getCwd()));
		}
		if (args.getEnv() != null) {
			Set<Entry<String, String>> entrySet = args.getEnv().entrySet();
			for (Entry<String, String> entry : entrySet) {
				String name = entry.getKey();
				String value = entry.getValue();
				if (value == null) {
					processBuilder.environment().remove(name);
				} else {
					processBuilder.environment().put(name, value);
				}
			}
		}
		try {
			if (DSPPlugin.DEBUG) {
				System.out.println("Launching: " + cmd);
			}
			Process start = processBuilder.start();
			DebugPlugin.newProcess(launch, start, cmd.toString());

			try {
				// TODO the python adapter (that uses the runInTerminal feature) does not like
				// to be told too early that the process is ready
				java.lang.Thread.sleep(1000);
			} catch (InterruptedException e) {
				java.lang.Thread.interrupted();
			}
			RunInTerminalResponse response = new RunInTerminalResponse();
			// TODO, no standard way of getting ID. Can via reflection and/or custom
			// launcher like CDT does.
			response.setProcessId(null); // Explicitly indicate we don't know the process id
			return CompletableFuture.completedFuture(response);
		} catch (IOException e) {
			// TODO
			return CompletableFuture.completedFuture(null);
		}

	}

	@Override
	public void breakpoint(BreakpointEventArguments args) {
		breakpointManager.breakpointEvent(args);
	}

	@Override
	public void breakpointAdded(IBreakpoint breakpoint) {
		breakpointManager.breakpointAdded(breakpoint);
	}

	@Override
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
		breakpointManager.breakpointRemoved(breakpoint, delta);
	}

	@Override
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		breakpointManager.breakpointChanged(breakpoint, delta);
	}

	@Override
	public IDebugProtocolServer getDebugProtocolServer() {
		return debugProtocolServer;
	}

	public Capabilities getCapabilities() {
		return capabilities;
	}
}
