/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.controlledbranchingnovember2020.server.rampup;

import static ca.mcmaster.controlledbranchingnovember2020.Constants.*;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import java.util.Map;

/**
 *
 * @author tamvadss
 */
public class RampupBranchCallback extends IloCplex.BranchCallback {

    @Override
    protected void main() throws IloException {
        //
        if  ( getNbranches()> ZERO ){ 
            
            RampupAttachment this_nodes_attachment = (RampupAttachment) getNodeData ( );
            if (null==this_nodes_attachment) this_nodes_attachment =new RampupAttachment();
            
            //default branch and record bracnhing condition in the leaf
            //
            
            IloNumVar[][] vars = new IloNumVar[TWO][] ;
            double[ ][] bounds = new double[TWO ][];
            IloCplex.BranchDirection[ ][]  dirs = new  IloCplex.BranchDirection[ TWO][];
            getBranches(  vars, bounds, dirs);
            
            //now allow  both kids to spawn
            for (int childNum = ZERO ;childNum<getNbranches();  childNum++) {   

                IloNumVar var = vars[childNum][ZERO];
                double bound = bounds[childNum][ZERO];
                IloCplex.BranchDirection dir =  dirs[childNum][ZERO];     

                boolean isDownBranch = dir.equals(   IloCplex.BranchDirection.Down);
                VariableAndBound vb = new VariableAndBound (var, bound) ;

                RampupAttachment attach = new RampupAttachment() ;
                attach.varFixings.put (vb, isDownBranch) ;
                
                for (Map.Entry<VariableAndBound , Boolean> entry : this_nodes_attachment.varFixings.entrySet()){
                    //
                    attach.varFixings.put  (entry.getKey(), entry.getValue());
                }

                //create the kid
                IloCplex.NodeId  id = makeBranch(var,bound, dir ,getObjValue(), attach);

                System.out.println("Node " + getNodeId() + " created " + id + " isdown " + isDownBranch + " var " + var.getName()) ;

            }  
             
        }
    }
    
}