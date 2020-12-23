/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.controlledbranchingnovember2020.subtree.callbacks;
 
import static ca.mcmaster.controlledbranchingnovember2020.Constants.*;
import ca.mcmaster.controlledbranchingnovember2020.subtree.NodeAttachment;
import ca.mcmaster.controlledbranchingnovember2020.subtree.TreeStructureNode;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author tamvadss
 */
public class LeafEnumerateNodecallback extends IloCplex.NodeCallback {
    
    public Set<TreeStructureNode> leafNodeAttahments = new HashSet <TreeStructureNode> (  );
     
    protected void main() throws IloException {
        //
        final long LEAFCOUNT =getNremainingNodes64();
        if (LEAFCOUNT>ZERO) {
            for (long leafNum = ZERO; leafNum < LEAFCOUNT; leafNum ++){
                TreeStructureNode treeNode = new TreeStructureNode ();
                
                boolean hasInformation = false;
                try {
                    treeNode.nodeID=getNodeId(leafNum) ;
                    treeNode.nodeAttachment = null;
                    treeNode.nodeAttachment =(NodeAttachment)getNodeData(  leafNum );
                    treeNode.lpRelaxObjective = getObjValue (leafNum) ;
                    
                    if (null!=  treeNode.nodeAttachment) hasInformation = true; 
                    
                }catch (Exception ex){
                    //ignore this leaf                    
                }
                                
                if (hasInformation) leafNodeAttahments.add (treeNode );
                    
            }
        }
        
        /*System.out.println("printing leafs");
        for (TreeStructureNode  leaf :leafNodeAttahments){
            System.out.println(leaf.nodeID);
        }*/
        
        abort();
    }
    
}
