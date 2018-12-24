# GDB Debug Adapter and GUI Client
GDB implementation of Debug Adapter Protocol as a proof of concept. This includes the GDB DAP server and a GDB Debugger GUI client.

## TODO

## BACKLOG
* pause flow
* step into flow
* step return flow
* debug target and server cleanup when debugging has finished
* implement editor features: line select on thread or stackframe select and on breakpoint
* when program is running variables panel needs to be cleared
* when program is running threads panel needs to only show root node (application)
* when program ends variables and threads panel need to be cleared
* add button to kill debug process
* cleanup console output
* finish up handling of running events ^running and *running
* track whether or not debuggee is stopped or running
* track current thread
* debugTarget should take arguments from user, e.g. target, attach or launch, working dir, etc.
* debugTarget should take input and output streams as args
* figure out (understand how it works) how to override threadpool like in DSPDebugTarget
* cleanup params and args arraylist format for commands
* cleanup OutputParser

## CLIENT BEHAVIOR
### Threads View
* selecting stack frame updates variables view, any other selection in view clears out variables view
* selecting stack frame jumps editor to line in source code (highlight entire line)
* breakpoint hit event selects top stack frame of active thread

### Debug Target Execution
* step (over, into, return) execute on active thread where breakpoint was hit
* step return moves one down the stack trace

### Console View
* displays debug target stdout only

### Editor View
* breakpoint hit event scroll to linenumber in top stack frame and highlight code from start of line to end of code
* breakpoint icons live to the left of the line numbers

## Resources
GDB: https://sourceware.org/gdb/onlinedocs/gdb/index.html#Top

DAP Specification: https://microsoft.github.io/debug-adapter-protocol/specification
