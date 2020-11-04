package gov.nasa.jpf.jdart.constraints.tree.internal;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.vm.Instruction;

class DecisionNode extends Node {

    private final Instruction branchInsn;
    private final Expression<Boolean>[] constraints;
    private final Node[] children;

    DecisionNode(DecisionNode parent, Instruction branchInsn, Expression<Boolean>[] constraints, int pos,
                 boolean explore, ExplorationStrategy strategy) {
        super(parent, pos);

        this.branchInsn = branchInsn;
        this.constraints = constraints;
        this.children = new Node[constraints.length];

        if (!explore) {
            for (int i = 0; i < constraints.length; i++) {
                this.children[i] = LeafNode.skipped(this, i);
            }
        } else {
            for (int i = 0; i < constraints.length; i++) {
                this.children[i] = LeafNode.open(this, i);
                strategy.newOpen( (LeafNode) this.children[i] );
            }
        }
    }

    Expression<Boolean> getConstraint(int idx) {
        return constraints[idx];
    }

    Node getChild(int idx) {
        return children[idx];
    }

    @Override
    boolean isDecisionNode() {
        return true;
    }

    void expand(LeafNode leaf, DecisionNode newChild) {
        children[leaf.childId()] = newChild;
    }

    void replace(LeafNode oldLeaf, LeafNode newLeaf) {
        children[oldLeaf.childId()] = newLeaf;
    }

    /**
     *
     * @param values
     * @return -1 if no constraint is satisfied
     * @throws RuntimeException e.g. due to function with undefined semantics
     */
    int evaluate(Valuation values) throws RuntimeException {
        for (int i = 0; i < constraints.length; i++) {
            Expression<Boolean> constraint = constraints[i];
            if (constraint.evaluate(values)) {
                return i;
            }
        }
        return -1;
    }

    String validateDecision(Instruction branchInsn, Expression<Boolean>[] constraints) {
        if (branchInsn != this.branchInsn) {
            return "Same decision, but different branching instruction!";
        }
        if (constraints != null && constraints.length != this.constraints.length) {
            return "Same decision, but different number of constraints!";
        }
        return null;
    }

    void print(StringBuilder out, int indent) {
        for (int i=0; i< children.length; i++) {
            indent(out, indent);
            out.append(i).append(" : ").append(constraints[i]).append("\n");
            children[i].print(out, indent + 1);
        }
    }
}
