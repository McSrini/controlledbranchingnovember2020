/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.controlledbranchingnovember2020.drivers;

import static ca.mcmaster.controlledbranchingnovember2020.Constants.*;
import static ca.mcmaster.controlledbranchingnovember2020.Parameters.*;
import ca.mcmaster.controlledbranchingnovember2020.subtree.*;
import ca.mcmaster.controlledbranchingnovember2020.subtree.lca.*;
import ca.mcmaster.controlledbranchingnovember2020.utils.LCA_Utils;
import ilog.cplex.IloCplex;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 */
public class TestDriver {
    
    public static void main(String[] args) throws Exception {
        
        //init with 2 fixings
        Map<Lite_VariableAndBound , Boolean> init =  new HashMap<Lite_VariableAndBound , Boolean>() ;
        IloCplex cplex = new IloCplex();
        cplex.importModel(   PRESOLVED_MIP_FILENAME);
        
        //Map<String, IloNumVar>  vars =  CplexUtils.getVariables(cplex);
        
        Lite_VariableAndBound vb1 = new Lite_VariableAndBound (("x453"), 0);
        Lite_VariableAndBound vb2 = new Lite_VariableAndBound (("x454"), 1);
         
        init.put (vb1, true) ;
        init.put (vb2, false);
        
        Subtree_LCA subtreeLCA = new Subtree_LCA (  new HashMap<Lite_VariableAndBound , Boolean>() , 0 , null);
        
        //solve for some time
        subtreeLCA.solve(BILLION, SOLUTION_CYCLE_TIME_SECONDS*THOUSAND -44100);
        
        //get and print tree structure
        subtreeLCA.root.printMe();
        
        //get LCA nodes and print them
        TreeMap    < Long , ArrayList< TreeStructureNode>  >  lcaNodes = subtreeLCA.collectLCANodes();
        for (Map.Entry  < Long , ArrayList< TreeStructureNode>  > entry: lcaNodes.entrySet()){
            System.out.println("\n\n\n\n\n\nSize" + entry.getKey()) ;
            for (TreeStructureNode tsNode : entry.getValue()){
                LCA_Utils.convertToLiteLCA(tsNode, subtreeLCA.myRoot_VarFixings).printMe ();
            }
        }
        
        System.out.flush();
        
        //prune leafs for 1 LCA node
        long wantedSize = 3;
        TreeStructureNode tsNode  =lcaNodes.get(wantedSize).get(ZERO);
        System.out.println("\n\n\n\n\n\nSelected for pruning..." );
        System.out.flush();
        Lite_LCA_Node newLiteLCA =   LCA_Utils.convertToLiteLCA(tsNode, subtreeLCA.myRoot_VarFixings);
        newLiteLCA.printMe ();
        Set < IloCplex.NodeId> pruneTragets = subtreeLCA.getLeafSetForLCANode(tsNode);
        for (IloCplex.NodeId nid : pruneTragets){
            System.out.println(" \nprune target " + nid+"," );
        }
        subtreeLCA.prune( pruneTragets  );
        
        System.out.println("\n\n\n\n\n\nCreating new tree..." );
        System.out.flush();
        
        Subtree_LCA newSubtreeLCA = new Subtree_LCA (  newLiteLCA.varFixings  , 0 , tsNode.nodeAttachment.branchingOverrules_for_Spwaning_Kids);
        
        
        //solve some more
        newSubtreeLCA.solve(BILLION, SOLUTION_CYCLE_TIME_SECONDS*THOUSAND -18000);
        
        System.out.println("\n\n\n\n\nget teree structure..." );
        System.out.flush();
        
        //get LCA nodes and print them
        //root = newSubtreeLCA.getTreeStructure();
        //root.printMe();
        
        lcaNodes = newSubtreeLCA.collectLCANodes();
        for (Map.Entry  < Long , ArrayList< TreeStructureNode >  > entry: lcaNodes.entrySet()){
            
            System.out.println("\n\n\nSize" + entry.getKey()) ;
            for (TreeStructureNode thisnode :  entry.getValue() ){
                LCA_Utils.convertToLiteLCA (thisnode, newSubtreeLCA.myRoot_VarFixings).printMe ();
            }
        }
        
    }
    
}
