package com.sun.squawk.traces;

/**
 * A <code>ExecutionPoint</code> describes the method and line number context of a sampled point of execution.
 */
class ExecutionPoint {

    /**
     * The method in which the execution point is located.
     */
    public final Symbols.Method method;

    /**
     * The source code line number of the execution point.
     */
    public final int lineNumber;

    /**
     * Creates an object representing a specified execution point.
     *
     * @param method     the method in which the execution point is located
     * @param lineNumber the line number of the execution point
     */
    public ExecutionPoint(Symbols.Method method, int lineNumber) {
        this.method = method;
        this.lineNumber = lineNumber;
    }

    /**
     * Determines if a given object is equal to this one.
     *
     * @return true iff <code>obj</code> is an ExecutionPoint instance and its {@link #method} and {@link #lineNumber}
     *         field values are equal to this object
     */
    public boolean equals(Object obj) {
        if (obj instanceof ExecutionPoint) {
            ExecutionPoint ep = (ExecutionPoint)obj;
            return ep.lineNumber == lineNumber && ep.method == method;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return method.getSignature().hashCode() + lineNumber;
    }

    /**
     * {@inheritDoc}
     *
     * @return the source code position of this execution point as 'file':'line number'
     */
    public String toString() {
        return method.getFile() + ":" + lineNumber;
    }
}

