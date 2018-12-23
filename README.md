# GDB Debug Adapter and GUI Client
GDB implementation of Debug Adapter Protocol as a proof of concept. This includes the GDB DAP server and a GDB Debugger GUI client.

### TODO
* implement editor features: line select on thread or stackframe select and on breakpoint

### BACKLOG
* editor panel should not scroll to bottom each time it updates
* continue should key off selected thread in threads panel
* add visible label? to denote where in the source code debugger has stopped
* when program is running variables and threads panel need to update accordingly
* when program ends variables and threads panel need to update accordingly
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

GDB:
https://sourceware.org/gdb/onlinedocs/gdb/index.html#Top

DAP Specification:
https://microsoft.github.io/debug-adapter-protocol/specification
