/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.controlledbranchingnovember2020.subtree;

/**
 *
 * @author tamvadss
 */
public class NodeAttachment {
    public NodeAttachment parentNode = null;
    //am I the down child of my parent ?
    public boolean am_I_The_Down_Branch_Child = false;
    
    public BranchingOverrule branchingOverrules_for_Spwaning_Kids = null;
    
    public NodeAttachment(){
        
    }
    
    public NodeAttachment (NodeAttachment parentNode , boolean am_I_The_Down_Branch_Child , 
            BranchingOverrule branchingOverrule ){
        
        this.am_I_The_Down_Branch_Child=am_I_The_Down_Branch_Child;
        this.parentNode=parentNode;
        this.branchingOverrules_for_Spwaning_Kids=branchingOverrule;
        
    }
}
