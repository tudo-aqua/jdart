package gov.nasa.jpf.jdart.constraints.tree.internal;

import gov.nasa.jpf.constraints.api.Valuation;

class LeafNode extends Node {

    static enum NodeType { OK, ERROR, DIVERGED, UNSAT, DONT_KNOW, OPEN, SKIPPED, BUGGY};

    private final NodeType type;

    private boolean complete = true;

    LeafNode(DecisionNode parent, NodeType type, int pos) {
        super(parent, pos);
        this.type = type;
    }

    NodeType nodeType() {
        return type;
    }

    /**
     * Dont_know is not exhausted, all other forms of data are
     *
     * @return
     */
    boolean isExhausted() {
        return  type != NodeType.DONT_KNOW &&
                type != NodeType.DIVERGED &&
                type != NodeType.OPEN;
    }

    /**
     * nodes that we do not want to replace
     * @return
     */
    boolean isFinal() {
        return type == NodeType.BUGGY ||
               type == NodeType.OK ||
               type == NodeType.ERROR;
    }

    @Override
    boolean isDecisionNode() {
        return false;
    }

    boolean complete() {
        return complete;
    }

    void setComplete(boolean c) {
        complete = c;
    }

    static LeafNode skipped(DecisionNode parent, int pos) {
        return new LeafNode(parent, NodeType.SKIPPED, pos);
    }

    static LeafNode open(DecisionNode parent, int pos) {
        return new LeafNode(parent, NodeType.OPEN, pos);
    }

    static LeafNode dontKnow(DecisionNode parent, int pos) {
        return new LeafNode(parent, NodeType.DONT_KNOW, pos);
    }

    static LeafNode unsat(DecisionNode parent, int pos) {
        return new LeafNode(parent, NodeType.UNSAT, pos);
    }

    static LeafWithValuation diverged(DecisionNode parent, int pos, Valuation val) {
        return new LeafWithValuation(parent, NodeType.DIVERGED, pos, val);
    }

    public String toString() {
        return "" + type + "[complete path:" + complete + "]";
    }

    void print(StringBuilder out, int indent) {
        indent(out, indent);
        out.append(type).append("\n");
    }
}
