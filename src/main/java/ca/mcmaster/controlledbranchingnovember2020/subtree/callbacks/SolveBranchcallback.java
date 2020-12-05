/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.controlledbranchingnovember2020.subtree.callbacks;

import static ca.mcmaster.controlledbranchingnovember2020.Constants.*;
import ca.mcmaster.controlledbranchingnovember2020.subtree.BranchingOverrule;
import ca.mcmaster.controlledbranchingnovember2020.subtree.Lite_VariableAndBound;
import ca.mcmaster.controlledbranchingnovember2020.subtree.NodeAttachment;
import ca.mcmaster.controlledbranchingnovember2020.subtree.SubTree;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author tamvadss
 */
public class SolveBranchcallback extends IloCplex.BranchCallback {
    
    protected void main() throws IloException {
        if ( getNbranches()> ZERO ){  
            String thisNodeID=getNodeId().toString();
                                   
            
            if (SubTree.pruneSet .size()> ZERO  && SubTree.pruneSet.remove( getNodeId())){
                //pruneSet is always empty in multi threaded solve mode
                //System.out.println("pruning "+ getNodeId()) ;
                prune ();                      

            }else {
                //branch
                //get the branches about to be created
                IloNumVar[][] vars = new IloNumVar[TWO][] ;
                double[ ][] bounds = new double[TWO ][];
                IloCplex.BranchDirection[ ][]  dirs = new  IloCplex.BranchDirection[ TWO][];
                getBranches(  vars, bounds, dirs);

                if (thisNodeID.equals( MIPROOT_NODE_ID)){
                    //root node
                    NodeAttachment attachment = new   NodeAttachment ( );
                    setNodeData (attachment );
                    attachment.branchingOverrules_for_Spwaning_Kids=SubTree.myRootOverrule;
                } 

                NodeAttachment thisNodesAttachment = null;
                try {
                    thisNodesAttachment  = (NodeAttachment) getNodeData () ;
                }        catch (Exception ex){
                    //stays null
                }
                
                if (null==thisNodesAttachment){
                    //default cplex branch
                    for (int childNum = ZERO ;childNum<getNbranches();  childNum++) {   
                        IloNumVar var = vars[childNum][ZERO];
                        double bound = bounds[childNum][ZERO];
                        IloCplex.BranchDirection dir =  dirs[childNum][ZERO]; 
                        IloCplex.NodeId  kid =makeBranch(var,bound, dir ,getObjValue());
                        
                        boolean isDownBranch = dir.equals(   IloCplex.BranchDirection.Down);     
                        
                        System.out.println("Node " + getNodeId() + " created " + kid + " isdown " + isDownBranch + " var " + var.getName()) ;
                        
                    }
                }else if (  thisNodesAttachment.branchingOverrules_for_Spwaning_Kids==null) {
                    //default cplex branch, but record it
                    thisNodesAttachment.branchingOverrules_for_Spwaning_Kids=    new BranchingOverrule ();
                    for (int childNum = ZERO ;childNum<getNbranches();  childNum++) {   
                        IloNumVar var = vars[childNum][ZERO];
                        double bound = bounds[childNum][ZERO];
                        IloCplex.BranchDirection dir =  dirs[childNum][ZERO]; 
                        boolean isDownBranch = dir.equals(   IloCplex.BranchDirection.Down);     
                         
                        if (isDownBranch){
                                thisNodesAttachment.branchingOverrules_for_Spwaning_Kids.downBranchCondition=
                                    new HashMap<Lite_VariableAndBound, Boolean>  () ;
                                thisNodesAttachment.branchingOverrules_for_Spwaning_Kids.downBranchCondition.put
                                    (new Lite_VariableAndBound (var.getName(), bound)  , isDownBranch);
                        }else {
                                thisNodesAttachment.branchingOverrules_for_Spwaning_Kids.upBranchCondition=
                                    new HashMap<Lite_VariableAndBound, Boolean>  () ;
                                thisNodesAttachment.branchingOverrules_for_Spwaning_Kids.upBranchCondition.put
                                    (new Lite_VariableAndBound (var.getName(), bound)  , isDownBranch);
                        }
                           
                        NodeAttachment kidAttach = new NodeAttachment (thisNodesAttachment, isDownBranch, null );
                        IloCplex.NodeId  kid  = makeBranch(var,bound, dir ,getObjValue(), kidAttach); 
                        
                        System.out.println("Node " + getNodeId() + " created " + kid + " isdown " + isDownBranch + " var " + var.getName()) ;
                       
                    }
                }else {
                    for (int childNum = ZERO ;childNum<getNbranches();  childNum++) {   
                        IloCplex.BranchDirection dir =  dirs[childNum][ZERO]; 
                        boolean isDownBranch = dir.equals(   IloCplex.BranchDirection.Down);     
                        
                        BranchingOverrule overrule =
                                thisNodesAttachment.branchingOverrules_for_Spwaning_Kids.upChildsBranchingOverrule;
                        HashMap<Lite_VariableAndBound, Boolean>  compoundBranchingCondition= 
                                thisNodesAttachment.branchingOverrules_for_Spwaning_Kids.upBranchCondition;
                        if (isDownBranch) {
                            overrule=
                                thisNodesAttachment.branchingOverrules_for_Spwaning_Kids.downChildsBranchingOverrule;
                            compoundBranchingCondition= 
                                thisNodesAttachment.branchingOverrules_for_Spwaning_Kids.downBranchCondition;
                        }
                         
                        NodeAttachment kidAttach = new NodeAttachment (thisNodesAttachment, isDownBranch, overrule  );
                        IloCplex.NodeId  kid  = makeBranch( compoundBranchingCondition, kidAttach); 
                        
                        System.out.println("Node " + getNodeId() + " created " + kid + " isdown " + isDownBranch + " compound condition ") ;
                       
                    }
                }
               
                
                
            }//end else
            
        }//end get N branches
    }//end main
    
    private   IloCplex.NodeId  makeBranch ( HashMap<Lite_VariableAndBound, Boolean>  compoundBranchingCondition, 
            NodeAttachment kidAttach ) throws IloException {
        
        // branches about to be created
        int size = compoundBranchingCondition.size();
        IloNumVar[] vars = new IloNumVar[size] ;
        double[] bounds = new double[size];
        IloCplex.BranchDirection[ ]  dirs = new  IloCplex.BranchDirection[size];
        
        System.out.println("compound branch condition") ;
        
        int index = ZERO;
        for (Map.Entry<Lite_VariableAndBound, Boolean> entry : compoundBranchingCondition.entrySet()){
             
            vars[index] = SubTree.varMap.get( entry.getKey().varName);
            bounds[index] =  entry.getKey().bound;
            dirs[index] = entry.getValue() ? IloCplex.BranchDirection.Down : IloCplex.BranchDirection.Up;
            index ++;
            
            System.out.println("Var " + SubTree.varMap.get( entry.getKey().varName) + " dir " +entry.getValue() ) ;
             
        }
        
        
        
        return makeBranch (vars, bounds, dirs ,   getObjValue(), kidAttach);
    }
    
}//end class
