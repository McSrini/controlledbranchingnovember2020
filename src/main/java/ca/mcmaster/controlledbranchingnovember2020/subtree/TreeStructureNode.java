/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.controlledbranchingnovember2020.subtree;

import static ca.mcmaster.controlledbranchingnovember2020.Constants.*;
import ilog.cplex.IloCplex;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author tamvadss
 */
public class TreeStructureNode {
    
    //node id available only for each leaf
    public  IloCplex.NodeId  nodeID =null;
    
    public TreeStructureNode parent = null;
    public TreeStructureNode downBranchChild = null;
    public TreeStructureNode upBranchChild = null;
    public NodeAttachment nodeAttachment;
    public double lpRelaxObjective=  BILLION ;
    
    //leaf set available for root
    public Set < TreeStructureNode> leafSet =null;
    
    public long downBranch_nonLeaf_refcount = ZERO;
    public long downBranch_Leaf_refcount = ZERO;
    public long upBranch_Leaf_refcount = ZERO;
    public long upBranch_NonLeaf_refcount = ZERO;
    
    public  BranchingOverrule getBranchingOverRule(){
        return getBranchingOverRule (this);
    }
    
    /*public boolean isPerfect (){
        return (downBranch_nonLeaf_refcount + upBranch_NonLeaf_refcount +ONE) +ONE == 
                downBranch_Leaf_refcount + upBranch_Leaf_refcount;
    }*/
    
    //print uncompacted tree, left first
    public void printMe(){
        
        if ( ! isLeaf() ) {
            System.out.println("Down branch consition") ;
            for (Map.Entry<Lite_VariableAndBound, Boolean> entry  : this.nodeAttachment.branchingOverrules_for_Spwaning_Kids.downBranchCondition.entrySet()){
                 System.out.println(" " + entry.getKey().varName + " , " + entry.getValue());
            }
            if (null != this.downBranchChild) this.downBranchChild.printMe();

            System.out.println("up branch consition") ;
            for (Map.Entry<Lite_VariableAndBound, Boolean> entry  : this.nodeAttachment.branchingOverrules_for_Spwaning_Kids.upBranchCondition.entrySet()){
                 System.out.println(" " + entry.getKey().varName + " , " + entry.getValue());
            }
            if (null != this.upBranchChild) this.upBranchChild.printMe();
        }
        
       
    }
    
    public HashMap<Lite_VariableAndBound , Boolean> getVarFixings () {
        HashMap<Lite_VariableAndBound , Boolean> result = new HashMap<Lite_VariableAndBound , Boolean> ();
        
        TreeStructureNode current = this;
        while (current.parent != null){
            TreeStructureNode myParent = current.parent;
            HashMap<Lite_VariableAndBound, Boolean> branchingConditions ;
            if (current.nodeAttachment.am_I_The_Down_Branch_Child){
                branchingConditions = myParent.nodeAttachment.branchingOverrules_for_Spwaning_Kids.downBranchCondition;
            }else {
                branchingConditions = myParent.nodeAttachment.branchingOverrules_for_Spwaning_Kids.upBranchCondition;
            }
            
            for (Map.Entry<Lite_VariableAndBound, Boolean> entry :branchingConditions.entrySet()){
                result.put ( entry.getKey(), entry.getValue());
            }
            
            current = current.parent;
        }
        
       
        
        return result;
    }
    
    public boolean isLeaf (){
        return  this. downBranchChild== null &&  this.upBranchChild==null;
    }
    
    private  BranchingOverrule getBranchingOverRule (TreeStructureNode tsNode){
        BranchingOverrule bo = new BranchingOverrule ();
        
        bo.downBranchCondition = new HashMap<Lite_VariableAndBound, Boolean>  ();
        bo.upBranchCondition =  new HashMap<Lite_VariableAndBound, Boolean>  ();
        
        TreeStructureNode downBranchLCA = tsNode.getChildNode (bo.downBranchCondition, true) ;
        TreeStructureNode upBranchLCA = tsNode.getChildNode (bo.upBranchCondition, false) ;
       
        if (!downBranchLCA.isLeaf()) {
            /* its not a leaf */ 
            bo.downChildsBranchingOverrule=getBranchingOverRule (downBranchLCA) ;
        }
        
        if (!upBranchLCA.isLeaf()){
            bo.upChildsBranchingOverrule= getBranchingOverRule(upBranchLCA) ;
        }
        
        tsNode.nodeAttachment.branchingOverrules_for_Spwaning_Kids = bo;
        return bo;
    }
    
    private TreeStructureNode getChildNode (HashMap<Lite_VariableAndBound, Boolean> branchCondition, boolean isDownBranch){
        
        //System.out.println("getChildNode");
        
        TreeStructureNode result =null;
        if (isDownBranch){
            result = this.downBranchChild;
            accumulateBranchingCondition (branchCondition, 
                    this.nodeAttachment.branchingOverrules_for_Spwaning_Kids.downBranchCondition) ;
        }else {
            result = this.upBranchChild;
            accumulateBranchingCondition (branchCondition, 
                    this.nodeAttachment.branchingOverrules_for_Spwaning_Kids.upBranchCondition) ;
        }
        
        //if result is a leaf or nonleaf with both sides, return
        boolean cond1 = result.downBranch_Leaf_refcount==ZERO && result.upBranch_Leaf_refcount!=ZERO;
        boolean cond2 = result.downBranch_Leaf_refcount!=ZERO && result.upBranch_Leaf_refcount==ZERO;
        
        while (cond1 || cond2){
            //keep skipping to the side that is not fathomed
            if (result.downBranch_Leaf_refcount==ZERO){
                
                accumulateBranchingCondition (branchCondition, 
                    result.nodeAttachment.branchingOverrules_for_Spwaning_Kids.upBranchCondition) ;
                result = result.upBranchChild;
            }else {
               
                accumulateBranchingCondition (branchCondition, 
                    result.nodeAttachment.branchingOverrules_for_Spwaning_Kids.downBranchCondition) ;
                result = result.downBranchChild;
            }
            
            cond1 = result.downBranch_Leaf_refcount==ZERO && result.upBranch_Leaf_refcount!=ZERO;
            cond2 = result.downBranch_Leaf_refcount!=ZERO && result.upBranch_Leaf_refcount==ZERO;
            
        }
        
        //  System.out.println("getChildNode end");
        
        return result;
    }
    
    private void accumulateBranchingCondition(
        HashMap<Lite_VariableAndBound, Boolean> branchCondition,
        HashMap<Lite_VariableAndBound, Boolean> thisCondition    ){
        
        
        
        
        for (Map.Entry<Lite_VariableAndBound, Boolean> entry : thisCondition.entrySet()  ){
            
            branchCondition.put (entry.getKey(), entry.getValue() );
            
             
        }
     
        
        
    }
    
}
