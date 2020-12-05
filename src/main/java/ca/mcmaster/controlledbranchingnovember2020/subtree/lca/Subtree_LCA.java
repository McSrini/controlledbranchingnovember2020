/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.controlledbranchingnovember2020.subtree.lca;

import static ca.mcmaster.controlledbranchingnovember2020.Constants.*;
import static ca.mcmaster.controlledbranchingnovember2020.Parameters.*;
import ca.mcmaster.controlledbranchingnovember2020.subtree.BranchingOverrule;
import ca.mcmaster.controlledbranchingnovember2020.subtree.Lite_VariableAndBound;
import ca.mcmaster.controlledbranchingnovember2020.subtree.SubTree;
import ca.mcmaster.controlledbranchingnovember2020.subtree.TreeStructureNode;
import ca.mcmaster.controlledbranchingnovember2020.subtree.callbacks.SolveBranchcallback;
import static ca.mcmaster.controlledbranchingnovember2020.utils.LCA_Utils.*;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 */
public class Subtree_LCA extends SubTree {
    
    public int iterationsCompleted ;
     
    public TreeStructureNode root;
       
    public Subtree_LCA ( Map<Lite_VariableAndBound , Boolean> varFixings, int iterationsCompleted,  BranchingOverrule bo ) throws IloException {
        super (varFixings, bo) ;
        this. iterationsCompleted  =   iterationsCompleted ;
    }
    
    @Override
    public void solve (double cutoff, long time_used_up_for_pruning_millisec) throws IloException{
        
        long solveTimeRemaining_seconds = THOUSAND*SOLUTION_CYCLE_TIME_SECONDS-  time_used_up_for_pruning_millisec ;
        solveTimeRemaining_seconds = solveTimeRemaining_seconds /THOUSAND;
        
        if (solveTimeRemaining_seconds <= FIVE){
            //set to 1 minute
            solveTimeRemaining_seconds = FIVE;
        }
         
        cplex.setParam( IloCplex.Param.Threads, MAX_CPLEX_THREADS);
        cplex.clearCallbacks();
        cplex.use ( new SolveBranchcallback( ) );
        cplex.setParam( IloCplex.Param.TimeLimit,  solveTimeRemaining_seconds );
        if (cutoff < BILLION) cplex.setParam(IloCplex.Param.MIP.Tolerances.UpperCutoff, cutoff);
         
        
        cplex.solve();              
        bestBoundAchieved= cplex.getBestObjValue();
        if (cplex.getStatus().equals( IloCplex.Status.Feasible ) || cplex.getStatus().equals( IloCplex.Status.Optimal )) 
                bestSolutionFound =cplex.getObjValue();
        this.numNodesProcessed = cplex.getNnodes64();
          
        log_statistics(  ++ iterationsCompleted );
            
        if (isCompletelySolved(cutoff)) {
            end();
            root = null;
        } else {
            root = getTreeStructure ( );
        }
           
    }
    
   
    
    //start from root and find lca nodes on either side
    //the lca node with most leafs is split into 2 lca nodes
    //repeat till no lca nodes left , or number of lca nodes is Workers-1
    //
    //key is number of leafs
    public     TreeMap    < Long , ArrayList< TreeStructureNode>  > collectLCANodes  ( ){
        //
        TreeMap    < Long , ArrayList< TreeStructureNode>  > lcaNodes =  
                new TreeMap < Long , ArrayList< TreeStructureNode>  >();
        
        
        
        boolean isAtLeastOneSplit = false;
        
        if (root !=null ){
            clearRefCounts(root); 
            markRefCounts();
        
            //if root has only 1 kid, repeatedly replace   with kid
            TreeStructureNode highestLCA = gethighestLCA (root) ;
            
            long count = highestLCA.downBranch_Leaf_refcount + highestLCA.upBranch_Leaf_refcount;
            ArrayList< TreeStructureNode>  current = lcaNodes.get (count );
            if (null ==current) current = new ArrayList< TreeStructureNode> ();
            current.add (highestLCA) ;
            lcaNodes.put (count, current) ;
             
            
            while ( getCount (lcaNodes) < NUM_WORKERS-ONE ){
                TreeStructureNode nodeToSplit = getNodeToSplit(lcaNodes) ;
                if (nodeToSplit == null){
                    break;
                }else {
                    isAtLeastOneSplit= true;
                    split (nodeToSplit, lcaNodes) ; 
                }
            }
            
        }
        
        return  isAtLeastOneSplit ? lcaNodes :  new TreeMap < Long , ArrayList< TreeStructureNode>  >();
    }
    
    public Set < IloCplex.NodeId> getLeafSetForLCANode (TreeStructureNode lcaNode){
        Set < IloCplex.NodeId> result = new HashSet < IloCplex.NodeId>();
        if (lcaNode.upBranchChild==null && lcaNode.downBranchChild ==null){
            //collect node ID of leaf
            result.add (lcaNode.nodeID ) ;            
        }else {
            if (null!=lcaNode.downBranchChild) result.addAll( getLeafSetForLCANode(lcaNode.downBranchChild));
            if (null!=lcaNode.upBranchChild) result.addAll(getLeafSetForLCANode(lcaNode.upBranchChild) );
        }
        return result;        
    }
    
    private int getCount (Map    < Long , ArrayList< TreeStructureNode>  > lcaNodes){
        int count = ZERO;
        for (ArrayList< TreeStructureNode> list : lcaNodes.values()){
            count += list.size();
        }
        return count;
    }
    
    private TreeStructureNode gethighestLCA(TreeStructureNode nonleafNode){
        TreeStructureNode result = nonleafNode;
        if (nonleafNode.upBranch_Leaf_refcount==ZERO || nonleafNode.downBranch_Leaf_refcount==ZERO){
            if (nonleafNode.upBranch_Leaf_refcount==ZERO){
                result = gethighestLCA (nonleafNode.downBranchChild) ;
            }else {
                result = gethighestLCA (nonleafNode.upBranchChild) ;
            }
        }
        return result;
        
    }
    
    private void clearRefCounts(TreeStructureNode subtreeRoot){
        if (subtreeRoot.downBranchChild!= null || subtreeRoot.upBranchChild!= null){
            subtreeRoot.downBranch_Leaf_refcount = ZERO;
            subtreeRoot.downBranch_nonLeaf_refcount = ZERO;
            subtreeRoot.upBranch_Leaf_refcount = ZERO;
            subtreeRoot.upBranch_NonLeaf_refcount= ZERO;
            
            if (subtreeRoot.downBranchChild!= null){
                clearRefCounts(subtreeRoot.downBranchChild);
            }
            if (subtreeRoot.upBranchChild!= null){
                clearRefCounts (subtreeRoot.upBranchChild) ; 
            }
        }        

    }
    
    private  void  markRefCounts (){
        
        if (null!=root){
            Set < TreeStructureNode> leafSet=root.leafSet;
            for (TreeStructureNode leaf : leafSet){
                //
                boolean isLeaf = true;
                TreeStructureNode current = leaf;
                TreeStructureNode parent = current.parent;
                
                while (null!= parent){
                    
                    if (!isLeaf){
                        long nonLeafRefcount = current.downBranch_nonLeaf_refcount + current.upBranch_NonLeaf_refcount;
                        if (current.nodeAttachment.am_I_The_Down_Branch_Child){
                            parent.downBranch_nonLeaf_refcount = ONE + nonLeafRefcount;
                        }else {
                            parent.upBranch_NonLeaf_refcount = ONE + nonLeafRefcount;
                        }
                        
                        //add to leaf set on both sides
                        if (current.nodeAttachment.am_I_The_Down_Branch_Child){
                            parent.downBranch_Leaf_refcount =current.downBranch_Leaf_refcount +  current.upBranch_Leaf_refcount;
                                    
                        }else {
                            parent.upBranch_Leaf_refcount =  current.downBranch_Leaf_refcount +  current.upBranch_Leaf_refcount;
                          
                        }
                        
                    } else {
                        
                        //add to leaf set on both sides
                        if (current.nodeAttachment.am_I_The_Down_Branch_Child){
                            parent.downBranch_Leaf_refcount = ONE;
                        }else {
                            parent.upBranch_Leaf_refcount = ONE;
                        }
                        
                    }
                    
                    //climb up
                    current = parent;
                    parent= parent.parent;
                    isLeaf = false;
                    
                }//end while
                
            }//end for
        }   
      
    }
}
