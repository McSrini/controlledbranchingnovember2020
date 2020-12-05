/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.controlledbranchingnovember2020.subtree.callbacks;
  
import static ca.mcmaster.controlledbranchingnovember2020.Constants.ZERO;
import ca.mcmaster.controlledbranchingnovember2020.subtree.SubTree;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author tamvadss
 */
public class PruneNodecallback extends IloCplex.NodeCallback {
    
    protected void main() throws IloException {
        //
        final long LEAFCOUNT =getNremainingNodes64();
        
        if (LEAFCOUNT>ZERO   ) {
            
            IloCplex.NodeId pruneTarget =  getPruneTarget();
            if (pruneTarget!=null){
                selectNode ( getNodeNumber64( pruneTarget )  ) ;
                //System.out.println(" node selected " + pruneTarget ) ;
            }else {
                abort ();
            }
            
        }
    }
    
    //sometimes, cplex silenty prunes infeasible nodes we were planning to prune explicitly
    private  IloCplex.NodeId   getPruneTarget () {
        IloCplex.NodeId  result = null;
         
        for (IloCplex.NodeId nd: SubTree.pruneSet){
            try  {
                getNodeNumber64(nd);
                result =nd ;                
                break;
            } catch (IloException iloEx) {
                //System.out.println(" no longer exists -------------- " + nd );
            }
        }
        return result;
    }
}
