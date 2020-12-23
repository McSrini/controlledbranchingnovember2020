/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.controlledbranchingnovember2020.utils;

import static ca.mcmaster.controlledbranchingnovember2020.Constants.ONE;
import static ca.mcmaster.controlledbranchingnovember2020.Constants.TWO;
import static ca.mcmaster.controlledbranchingnovember2020.Constants.ZERO;
import ca.mcmaster.controlledbranchingnovember2020.subtree.BranchingOverrule;
import ca.mcmaster.controlledbranchingnovember2020.subtree.Lite_VariableAndBound;
import ca.mcmaster.controlledbranchingnovember2020.subtree.TreeStructureNode;
import ca.mcmaster.controlledbranchingnovember2020.subtree.lca.Lite_LCA_Node;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 */
public class LCA_Utils {
    
    //find the lca nodes on either side, and insert into map
    public static void split (TreeStructureNode nodeToSplit,  TreeMap    < Long , ArrayList< TreeStructureNode>  > lcaNodes ) {
        
        TreeStructureNode upChild = nodeToSplit.upBranchChild;
        TreeStructureNode downChild = nodeToSplit.downBranchChild;
        
        TreeStructureNode upBranchLCA = skip_NonLeafNode_with1Child (upChild);
        TreeStructureNode downBranchLCA = skip_NonLeafNode_with1Child (downChild) ; 
        
        long upBranchLCALeafCount = upBranchLCA.downBranch_Leaf_refcount + upBranchLCA.upBranch_Leaf_refcount;
        
        //System.out.println("upBranchLCALeafCount "+ upBranchLCALeafCount) ;
        
        if (upBranchLCALeafCount> ZERO){
            
            //not a leaf
            ArrayList< TreeStructureNode>  current=           lcaNodes.get (upBranchLCALeafCount) ;
            if (null == current) current = new ArrayList< TreeStructureNode> ();
            current.add (upBranchLCA );
            lcaNodes.put (upBranchLCALeafCount, current) ;
        }
        
        long downBranchLCALEafcount = downBranchLCA.downBranch_Leaf_refcount + downBranchLCA.upBranch_Leaf_refcount;
        
        //System.out.println("downBranchLCALEafcount "+ downBranchLCALEafcount) ;
        
        if (downBranchLCALEafcount> ZERO){
            //not a leaf
            ArrayList< TreeStructureNode>  current=           lcaNodes.get ( downBranchLCALEafcount) ;
            if (null == current) current = new ArrayList< TreeStructureNode> ();
            current.add (downBranchLCA);
            lcaNodes.put ( downBranchLCALEafcount, current) ;
        }
        
    }
    
    public static TreeMap    < Long , ArrayList< Lite_LCA_Node>  >  
        convertToLiteLCA (  TreeMap    < Long , ArrayList< TreeStructureNode>  > lcaNodes,
                            Map<Lite_VariableAndBound , Boolean> root_VarFixings){
            
            TreeMap    < Long , ArrayList< Lite_LCA_Node>  >   result = 
                    new TreeMap    < Long , ArrayList< Lite_LCA_Node>  >  ();
            
            for (Map.Entry    < Long , ArrayList< TreeStructureNode>  > entry: lcaNodes.entrySet()){
                //
                ArrayList< Lite_LCA_Node>  current = new ArrayList< Lite_LCA_Node>();
                for (TreeStructureNode tsNode : entry.getValue()){
                    current.add ( convertToLiteLCA (tsNode, root_VarFixings));    
                }
                result.put (entry.getKey(), current );
            }
            
            return result;
        
    }
    
    //must have more than 2 kids
    public static  TreeStructureNode getNodeToSplit( TreeMap    < Long , ArrayList< TreeStructureNode>  > lcaNodes ) {
        TreeStructureNode result = null;
        long succesfulKey = -ONE;
        
        /*System.out.println("\n\n\n\n printing lca node keyset") ;
        for (Map.Entry    < Long , ArrayList< TreeStructureNode>  > entry: lcaNodes.entrySet()){
            System.out.println("keySize "+ entry.getKey() + " value " + entry.getValue().size()) ;  
        }*/
        
        //for ( Map.Entry    < Long , ArrayList< TreeStructureNode>  >  entry: lcaNodes.descendingMap().entrySet()){
        for ( Map.Entry    < Long , ArrayList< TreeStructureNode>  >  entry: lcaNodes .entrySet()){
            
            if (TWO >= entry.getKey()) continue;
            
            ArrayList< TreeStructureNode> current = entry.getValue();
            result = getNodeToSplit (current) ;
            if (null!=result) {   
                succesfulKey =entry.getKey();
                break;
            }
        }
        
        if (succesfulKey>ZERO){
            if (lcaNodes.get(succesfulKey).size()==ZERO){
                lcaNodes.remove( succesfulKey);
            }
        }
        
        return result;
    }
    
    public  static Lite_LCA_Node convertToLiteLCA (TreeStructureNode tsNode, 
            Map<Lite_VariableAndBound , Boolean> root_VarFixings){
        Lite_LCA_Node result = new Lite_LCA_Node();
        
        //result.lpRelax = tsNode.lpRelaxObjective;
        result.numLeafsRepresented= tsNode.downBranch_Leaf_refcount+ tsNode.upBranch_Leaf_refcount; 
        result.varFixings  = tsNode.getVarFixings();  
        
        for (Map.Entry<Lite_VariableAndBound , Boolean> entry : root_VarFixings.entrySet()){
            result.varFixings.put( entry.getKey(), entry.getValue() );
        }
         
        /*if (! tsNode.isPerfect())*/ result.branchingOverrule = tsNode.getBranchingOverRule(); 
        
     
        return result;
    }
    
    private static TreeStructureNode getNodeToSplit ( ArrayList< TreeStructureNode> nodeList){
        int index = ZERO;
        for (; index < nodeList.size(); index ++){
            TreeStructureNode current = nodeList.get(index);
            if (current.downBranch_Leaf_refcount + current.upBranch_Leaf_refcount> TWO){
                break;
            }
        }
        return index < nodeList.size() ? nodeList.remove(index) : null;
    }
    
    private static TreeStructureNode skip_NonLeafNode_with1Child (TreeStructureNode nonLeafNode) {
        TreeStructureNode result = nonLeafNode;
        while (  true ){
            
            boolean isDownBranchFathomed = result.downBranch_Leaf_refcount==ZERO && result.upBranch_Leaf_refcount!= ZERO;
            boolean isUpBranchFathomed   = result.downBranch_Leaf_refcount!=ZERO && result.upBranch_Leaf_refcount== ZERO;
        
            if (!isDownBranchFathomed && !isUpBranchFathomed)  break;
             
            if (isUpBranchFathomed){
                result = result.downBranchChild;
            }else {
                result = result.upBranchChild;
            }
             
        }
        return result;
    }
    
}
