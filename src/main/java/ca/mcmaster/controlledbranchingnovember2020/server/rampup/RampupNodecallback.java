/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.controlledbranchingnovember2020.server.rampup;

import static ca.mcmaster.controlledbranchingnovember2020.Constants.*;
import static ca.mcmaster.controlledbranchingnovember2020.Parameters.*;
import ca.mcmaster.controlledbranchingnovember2020.subtree.Lite_VariableAndBound;
import ca.mcmaster.controlledbranchingnovember2020.subtree.lca.Lite_LCA_Node;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tamvadss
 */
public class RampupNodecallback extends IloCplex.NodeCallback {
    
    public List<Lite_LCA_Node> result = new ArrayList<Lite_LCA_Node>();

    @Override
    protected void main() throws IloException {
        //
       
        if (getNremainingNodes64()==NUM_WORKERS){
            //prepare leafs
            for (int leafNum = ZERO; leafNum < NUM_WORKERS ; leafNum ++){
                //
                RampupAttachment attach = (RampupAttachment) getNodeData (leafNum) ;
                Lite_LCA_Node lca = new Lite_LCA_Node ();
                
                System.out.println("node id is " + getNodeId(leafNum) ) ;
                
                //lca .varFixings = attach.varFixings;
                
                for (Map.Entry<VariableAndBound , Boolean>  entry : attach.varFixings.entrySet()){
                    Lite_VariableAndBound liteVb    = new Lite_VariableAndBound () ;
                    liteVb.varName= entry.getKey().getVar().getName();
                    liteVb.bound = entry.getKey().getBound();
                    lca .varFixings.put ( liteVb , entry.getValue());                    
                }
                
                
                result.add (lca );
            }

            abort();
        }
    }
    
}
