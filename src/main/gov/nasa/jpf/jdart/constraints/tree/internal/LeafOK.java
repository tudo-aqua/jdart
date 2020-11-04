package gov.nasa.jpf.jdart.constraints.tree.internal;

import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.jdart.constraints.paths.PostCondition;

public class LeafOK extends LeafWithValuation {

    final PostCondition postCondition;

    LeafOK(DecisionNode parent, int pos, Valuation val, PostCondition postCondition) {
        super(parent, NodeType.OK, pos, val);
        this.postCondition = postCondition;
    }

}
