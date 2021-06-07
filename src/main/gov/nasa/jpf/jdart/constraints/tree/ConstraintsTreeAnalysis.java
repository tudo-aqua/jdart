/*
 * Copyright (C) 2015, United States Government, as represented by the 
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 * 
 * The PSYCO: A Predicate-based Symbolic Compositional Reasoning environment 
 * platform is licensed under the Apache License, Version 2.0 (the "License"); you 
 * may not use this file except in compliance with the License. You may obtain a 
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0. 
 * 
 * Unless required by applicable law or agreed to in writing, software distributed 
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR 
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the 
 * specific language governing permissions and limitations under the License.
 */
package gov.nasa.jpf.jdart.constraints.tree;

import com.google.gson.Gson;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jpf.constraints.util.MixedParamsException;
import gov.nasa.jpf.jdart.constraints.paths.Path;
import gov.nasa.jpf.jdart.constraints.paths.PathResult;
import gov.nasa.jpf.jdart.constraints.paths.PathState;
import gov.nasa.jpf.util.JPFLogger;
import java.io.PrintWriter;
import java.util.*;

import com.google.common.base.Predicate;

/**
 * constraints tree implementation. 
 */
public class ConstraintsTreeAnalysis {

  private final ConstraintsTree constraintsTree;

  public ConstraintsTreeAnalysis(ConstraintsTree tree) {
    this.constraintsTree = tree;
  }

/*
    public void toJson(String filename) {
      JsonNode root = toJson(false, true);
      String tree = new Gson().toJson(root);

      try(PrintWriter out = new PrintWriter(filename)) {
        out.println(tree);
      } catch (Exception ex) {
        logger.severe("Could not write to json file: ", ex);
      }
    }

  public Node strip(Predicate<? super PathResult> pred) {
      if(isLeaf()) {
        if(pred.apply(path.getPathResult()))
          return null;
        return new Node(path.getPathCondition(), path.getPathResult());
      }
      
      Node trueStripped = succTrue.strip(pred);
      Node falseStripped = succFalse.strip(pred);
      if(trueStripped == null)
        return falseStripped;
      if(falseStripped == null)
        return trueStripped;
      return new Node(decision, trueStripped, falseStripped);
    }

    /**
     * compute constraints on path from node to root.
     * @return 
     * /
    public Expression<Boolean> getConstraint() {
      return getConstraint(null);
    }
    
    private Expression<Boolean> getConstraint(Node child) {

      Expression<Boolean> myDec = this.decision;
      if (myDec != null && child != null && child == succFalse) {
        myDec = new Negation(this.decision);
      }
      
      Expression<Boolean> pExpr = (parent == null) ? null : parent.getConstraint(this);
      if (pExpr == null) {
        return myDec;
      }
      
      if (this.decision == null) {
        return pExpr;
      }
      
      return new PropositionalCompound(pExpr, LogicalOperator.AND, myDec);
    }        
  }



  public String toString(boolean values, boolean postconditions) {
    return root.toString(values, postconditions);
  }

  public void toJson(String filename) {
    root.toJson(filename);
  }
*/
  /**
   * pretty print
   * 
   * @return 
   */
  @Override
  public String toString() {
    return constraintsTree.toString();
  }

  
  /* **************************************************************************
   * 
   * data retrieval
   * 
   */
  public Collection<LeafNode> getOpenLeafs() {
    return getNodesMatchingStates( EnumSet.of(LeafNode.NodeType.OPEN) );
  }

  public Collection<LeafNode> getOkLeafs() {
    return getNodesMatchingStates( EnumSet.of(LeafNode.NodeType.OK) );
  }
 
  public Collection<LeafNode> getErrorLeafs() {
    return getNodesMatchingStates( EnumSet.of(LeafNode.NodeType.ERROR) );
  }
  
  public Collection<LeafNode> getDontKnowLeafs() {
    return getNodesMatchingStates( EnumSet.of(LeafNode.NodeType.DONT_KNOW) );
  }

  public Collection<LeafNode> getSkippedLeafs() {
    return getNodesMatchingStates( EnumSet.of(LeafNode.NodeType.SKIPPED) );
  }

  public Collection<LeafNode> getBuggyLeafs() {
    return getNodesMatchingStates( EnumSet.of(LeafNode.NodeType.BUGGY) );
  }

  public Collection<LeafNode> getUnsatLeafs() {
    return getNodesMatchingStates( EnumSet.of(LeafNode.NodeType.UNSAT) );
  }

  public Collection<LeafNode> getDivergedLeafs() {
    return getNodesMatchingStates( EnumSet.of(LeafNode.NodeType.DIVERGED) );
  }

  /*
  public Collection<Path> getAllPaths() {
    ArrayList<Path> ret = new ArrayList<>();    
    for (Node n : getDoneLeaves()) {
      ret.add(n.path);
    }
    return ret;
  }

  public Set<Variable<?>> getVariables() {
    Set<Variable<?>> vars = new HashSet<Variable<?>>();
    Collection<Node> nodes = getDoneLeaves();
    for (Node n : nodes) {
      n.path.getPathCondition().collectFreeVariables(vars);
    }
    return vars;
  }
  
  public Expression<Boolean> getConstraint(Predicate<? super PathResult> pred, Predicate<? super Variable<?>> restrict) {
    return getConstraintInSubtree(pred, restrict, root);
  }
  
  
  public Expression<Boolean> getCoveredConstraint() {
//    return getConstraintForStateInSubtree(PathState.OK, root);
    return getConstraintsFor(PathState.OK);
  }
  
  public Expression<Boolean> getErrorConstraint() {
//    return getConstraintForStateInSubtree(PathState.ERROR, root);
    return getConstraintsFor(PathState.ERROR);
  }
  
  public Expression<Boolean> getDontKnowConstraint() {
//    return getConstraintForStateInSubtree(PathState.DONT_KNOW, root);
    return getConstraintsFor(PathState.DONT_KNOW);
  }

  public Expression<Boolean> getCoveredConstraintRestricted(Set<?> restrict) {
    return getConstraintForStateInSubtree(PathState.OK, root, restrict);
  }
  
  public Expression<Boolean> getErrorConstraintRestricted(Set<?> restrict) {
    return getConstraintForStateInSubtree(PathState.ERROR, root, restrict);
  }
  
  public Expression<Boolean> getDontKnowConstraintRestricted(Set<?> restrict) {
    return getConstraintForStateInSubtree(PathState.DONT_KNOW, root,restrict);
  }  
  
  public ConstraintsTreeAnalysis strip(Predicate<? super PathResult> resultPred) {
    Node newRoot = root.strip(resultPred);
    return (newRoot != null) ? new ConstraintsTreeAnalysis(newRoot) : null;
  }
  
  
  
  /* **************************************************************************
   * 
   * private helpers
   * 
   * /
  
  private Expression<Boolean> getConstraintInSubtree(Predicate<? super PathResult> pred,
      Predicate<? super Variable<?>> restrict,
      Node subtreeRoot) {
    if(subtreeRoot.isLeaf()) {
      return ExpressionUtil.boolConst(pred.apply(subtreeRoot.path.getPathResult()));
    }
    
    Set<Variable<?>> vars = ExpressionUtil.freeVariables(subtreeRoot.decision);
    boolean relevant = true;
    boolean someRelevant = false;
    for(Variable<?> v : vars) {
      if(!restrict.apply(v))
        relevant = false;
      else
        someRelevant = true;
    }
    
    if(!relevant && someRelevant)
      throw new MixedParamsException();
    
    Expression<Boolean> sf = getConstraintInSubtree(pred, restrict, subtreeRoot.succFalse);
    Expression<Boolean> st = getConstraintInSubtree(pred, restrict, subtreeRoot.succTrue);
    
    
    if(sf instanceof Constant) {
      boolean bf = ((Constant<Boolean>)sf).getValue();
      
      
      if(st instanceof Constant) {
        boolean bt = ((Constant<Boolean>)st).getValue();
        if(bf) {
          if(bt || !relevant)
            return ExpressionUtil.TRUE;
          return new Negation(subtreeRoot.decision);
        }
        if(bt) { // && !bf
          if(!relevant)
            return ExpressionUtil.TRUE;
          return subtreeRoot.decision;
        }
        // !bf && !bt
        return ExpressionUtil.FALSE;
      }
      
      if(bf)
        sf = relevant ? new Negation(subtreeRoot.decision) : ExpressionUtil.TRUE;
      else
        sf = null;
    }
    else {
      if(relevant)
        sf = new PropositionalCompound(new Negation(subtreeRoot.decision), LogicalOperator.AND, sf);
    }
    
    if(st instanceof Constant) {
      boolean bt = ((Constant<Boolean>)st).getValue();
      
      if(bt)
        st = relevant ? subtreeRoot.decision : ExpressionUtil.TRUE;
      else
        st = null;
    }
    else {
      if(relevant)
        st = new PropositionalCompound(subtreeRoot.decision, LogicalOperator.AND, st);
    }
    
    if(sf == null)
      return st;
    if(st == null)
      return sf;
    return new PropositionalCompound(sf, LogicalOperator.OR, st);
  }

  private Expression<Boolean> getConstraintForStateInSubtree(PathState state,
      Node subtreeRoot, Set<?> restrict) {

    if (subtreeRoot.isLeaf()) {

      return subtreeRoot.path.getState().equals(state) ? ExpressionUtil.TRUE
          : ExpressionUtil.FALSE;
    }

    Set<Variable<?>> vars = new HashSet<Variable<?>>();
    subtreeRoot.decision.collectFreeVariables(vars);
    boolean relevant = true;
    if (!restrict.containsAll(vars)) {
      relevant = false;
    }

    // check on children ...
    Expression<Boolean> sf = getConstraintForStateInSubtree(state,
        subtreeRoot.succFalse, restrict);
    Expression<Boolean> st = getConstraintForStateInSubtree(state,
        subtreeRoot.succTrue, restrict);

    // can we simplify?
    if (sf instanceof Constant && st instanceof Constant) {
      boolean bf = ((Constant<Boolean>) sf).getValue();
      boolean bt = ((Constant<Boolean>) st).getValue();

      if (bf && bt) {
        return ExpressionUtil.TRUE;
      }

      if (!bf && !bt) {
        return ExpressionUtil.FALSE;
      }

      if (bt) {
        return relevant ? subtreeRoot.decision : ExpressionUtil.TRUE;
      } else {
        return relevant ? new Negation(subtreeRoot.decision)
            : ExpressionUtil.TRUE;
      }
    }

    // simplify independenty
    if (sf instanceof Constant) {
      boolean bf = ((Constant<Boolean>) sf).getValue();
      if (bf) {
        sf = relevant ? new Negation(subtreeRoot.decision)
            : ExpressionUtil.TRUE;
      } else {
        sf = null;
      }
    } else {
      if (relevant)
        sf = new PropositionalCompound(new Negation(subtreeRoot.decision),
            LogicalOperator.AND, sf);
    }

    // simplify independenty
    if (st instanceof Constant) {
      boolean bt = ((Constant<Boolean>) st).getValue();
      if (bt) {
        st = relevant ? subtreeRoot.decision : ExpressionUtil.TRUE;
      } else {
        st = null;
      }
    } else {
      if (relevant)
        st = new PropositionalCompound(subtreeRoot.decision,
            LogicalOperator.AND, st);
    }

    if (sf == null || st == null) {
      return (sf != null) ? sf : st;
    }

    return new PropositionalCompound(sf, LogicalOperator.OR, st);
  }
    
  private Expression<Boolean> getConstraintsFor(PathState state) {
    LinkedList<Node> nodes = new LinkedList<Node>(getNodesMatchingState(state));
    if (nodes.isEmpty()) {
      return ExpressionUtil.FALSE;
    }
    Expression<Boolean> ret = nodes.pollLast().path.getPathCondition();
    while (!nodes.isEmpty()) {
      ret = new PropositionalCompound(ret, LogicalOperator.OR,
              nodes.pollLast().path.getPathCondition());
    }
    return ret;
  }

  */

  private Collection<LeafNode> getNodesMatchingStates(EnumSet<LeafNode.NodeType> states) {

    Collection<LeafNode> matching = new LinkedList<>();
    LinkedList<Node> worklist = new LinkedList<>();
    worklist.offer(constraintsTree.root());

    while (!worklist.isEmpty()) {
      Node n = worklist.poll();

      if (!n.isDecisionNode()) {
        LeafNode leaf = (LeafNode)n;
        if (states.contains( ((LeafNode) n).nodeType()) ) {
          matching.add(leaf);
        }
      } else {
        for ( Node c :  ((DecisionNode)n).children()) {
          worklist.offer(c);
        }
      }
    }
    return matching;
  }

  /*
  private Collection<Node> getDoneLeaves() {
    Collection<Node> result = new ArrayList<>();
    Node curr = root;
    Node prev = null;
    
    while (curr != null) {
      if (curr.isLeaf()) {
        result.add(curr);
        prev = curr; curr = curr.parent;
      }
      // down
      else if (prev == null || prev == curr.parent) {
        prev = curr; curr = curr.succTrue;
      }
      // from true to false
      else if (prev == curr.succTrue) {
        prev = curr; curr = curr.succFalse;
      }
      // up
      else {
        prev = curr; curr = curr.parent;
      }
    }
    
    return result;    
  }
  */

}
