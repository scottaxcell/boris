package com.boris.debug.server.mi.record;

/**
 * stream-record -> console-stream-output | target-stream-output | log-stream-output
 */
public class StreamRecord extends OutOfBandRecord {
    public static char CONSOLE_OUTPUT_PREFIX = '~';
    public static char TARGET_OUTPUT_PREFIX = '@';
    public static char LOG_OUTPUT_PREFIX = '&';

}
