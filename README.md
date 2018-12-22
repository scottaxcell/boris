# GDB Debug Adapter and GUI Client
GDB implementation of Debug Adapter Protocol as a proof of concept. This includes the GDB DAP server and a GDB Debugger GUI client.

### TODO
* align client debug model with eclipse cdt model better
* BUG - variables panel is not always updating, looks like threads panel is interferring with it's calls to the client/server

### BACKLOG
* editor panel should not scroll to bottom each time it updates
* variables panel should key off thread selected in threads panel
* continue should key off selected thread in threads panel
* next should key off selected thread in threads panel
* add visible label? to denote where in the source code debugger has stopped
* when program is running variables and threads panel need to update accordingly
* when program ends variables and threads panel need to update accordingly
* add button to kill debug process
* cleanup console output
* finish up handling of running events ^running and *running
* track whether or not debuggee is stopped or running
* track current thread
* client should take arguments from user, e.g. target, attach or launch, working dir, etc.
* client should take input and output streams as args
* figure out (understand how it works) how to override threadpool like in DSPDebugTarget
* cleanup params and args arraylist format for commands
* cleanup OutputParser

GDB:
https://sourceware.org/gdb/onlinedocs/gdb/index.html#Top

DAP Specification:
https://microsoft.github.io/debug-adapter-protocol/specification
