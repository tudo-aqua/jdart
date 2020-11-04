package gov.nasa.jpf.jdart.constraints.tree.internal;

import gov.nasa.jpf.constraints.api.Valuation;

class LeafWithValuation extends LeafNode {

    private Valuation values;

    LeafWithValuation(DecisionNode parent, LeafNode.NodeType type, int pos, Valuation val) {
        super(parent, type, pos);
        this.values = val;
    }

    Valuation values() {
        return values;
    }

    void updateValues(Valuation v) {
        values = v;
    }

    void print(StringBuilder out, int indent) {
        indent(out, indent);
        out.append(NodeType.OK).append("[complete path:").append(complete()).append("]").
                append(" . ").append( values() ).append("\n");
    }
}
