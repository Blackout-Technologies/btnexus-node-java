package ai.blackout.node;

/*
 * Some usefull things to handle exceptions
 */

//package jsslib.util;

/**
 *
 * @author robert schuster
 */
public class ExceptionHandling {

    /**
     * Convert the result of Exception.getStackTrace to a String
     * @param ex The exception to get the Stringified Stack trace from
     * @return
     */
    public static String StackTraceToString(Exception ex) {
        String result = ex.toString() + "\n";
        StackTraceElement[] trace = ex.getStackTrace();
        for (int i=0;i<trace.length;i++) {
            result += trace[i].toString() + "\n";
        }
        return result;
    }
}