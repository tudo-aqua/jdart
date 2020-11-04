package gov.nasa.jpf.jdart.constraints.tree.internal;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.jdart.constraints.paths.PathResult;

abstract class Node {

    private final DecisionNode parent;

    /**
     * depth in tree
     */
    private final int depth;

    /**
     *
     */
    private final int position;

    Node(DecisionNode parent, int childId) {
        this.parent = parent;
        this.depth = (parent != null) ? parent.depth() + 1 : 0;
        this.position = childId;
    }

    DecisionNode parent() {
        return parent;
    }

    int depth() {
        return depth;
    }

    int childId() {
        return position;
    }

    abstract boolean isDecisionNode();

    abstract void print(StringBuilder out, int indent);

    void indent(StringBuilder out, int indent) {
        for (int i=0; i<indent; i++) {
            out.append("  ");
        }
        out.append("+ ");
    }
}
