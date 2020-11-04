package gov.nasa.jpf.jdart.constraints.tree.internal;

import gov.nasa.jpf.constraints.api.Valuation;

class LeafError extends LeafWithValuation {

    private final String exceptionClass;
    private final String stackTrace;

    LeafError(DecisionNode parent, int pos, Valuation val, String exceptionClass, String stackTrace) {
        super(parent, NodeType.ERROR, pos, val);
        this.exceptionClass = exceptionClass;
        this.stackTrace = stackTrace;
    }

    void print(StringBuilder out, int indent) {
        indent(out, indent);
        out.append(NodeType.ERROR).append("[complete path:").append(complete()).append("]").
                append(" . ").append( values() ).append(" . ").
                append( exceptionClass ).append("\n");
    }
}
