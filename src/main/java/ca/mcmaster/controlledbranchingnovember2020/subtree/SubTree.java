/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.controlledbranchingnovember2020.subtree;

import static ca.mcmaster.controlledbranchingnovember2020.Constants.*;
import ca.mcmaster.controlledbranchingnovember2020.Parameters;
import static ca.mcmaster.controlledbranchingnovember2020.Parameters.*;
import ca.mcmaster.controlledbranchingnovember2020.subtree.callbacks.LeafEnumerateNodecallback;
import ca.mcmaster.controlledbranchingnovember2020.subtree.callbacks.PruneNodecallback;
import ca.mcmaster.controlledbranchingnovember2020.subtree.callbacks.SolveBranchcallback;
import ca.mcmaster.controlledbranchingnovember2020.utils.CPlexUtils;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import static java.lang.System.exit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class SubTree {
    protected IloCplex cplex = null;
    protected static Logger logger;
    
    public  Map<Lite_VariableAndBound , Boolean> myRoot_VarFixings = new HashMap<Lite_VariableAndBound , Boolean>();
    public static BranchingOverrule myRootOverrule = null;
    
    public boolean isCompletelySolved = false;
    public double bestBoundAchieved= BILLION;
    public double bestSolutionFound = BILLION;
    public long numNodesProcessed = ZERO;
    
    public  static Map<String, IloNumVar> varMap = null;
    
    //prune instruction
    public static Set<IloCplex.NodeId> pruneSet = new HashSet<IloCplex.NodeId>();
    
    static {
        logger=Logger.getLogger(SubTree.class);
        logger.setLevel(LOGGING_LEVEL);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa =new  
                RollingFileAppender(layout,LOG_FOLDER+SubTree.class.getSimpleName()+ LOG_FILE_EXTENSION);
            rfa.setMaxBackupIndex(SIXTY);
            logger.addAppender(rfa);
            logger.setAdditivity(false);            
             
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging"+ex);       
            exit(ONE);
        }
    }
   
    //argument is bounds, and is uppperBound?
    public SubTree ( Map<Lite_VariableAndBound , Boolean> varFixings, BranchingOverrule bo) throws IloException {
        
        cplex = new IloCplex();
        cplex.importModel(   PRESOLVED_MIP_FILENAME);
        CPlexUtils.setCplexConfig (cplex) ;
        
        myRootOverrule = bo;
        
        varMap = CPlexUtils. getVariables (  cplex);
         
        for (Map.Entry<Lite_VariableAndBound , Boolean> entry : varFixings.entrySet()){
            Lite_VariableAndBound vb = entry.getKey();
            IloNumVar var= varMap.get (vb.varName);
            double newBound= vb.bound;
            if (entry.getValue()){
                CPlexUtils.updateVariableBounds( var,   newBound, true  )   ;
            }else {
                CPlexUtils.updateVariableBounds( var,   newBound, false  )   ;
            }
            
            myRoot_VarFixings.put (new Lite_VariableAndBound (var.getName(), newBound) , entry.getValue()) ;
        }
    }
    

    //sequential solve
    public void solve ( double cutoff, long time_used_up_for_pruning_millisec ) throws IloException{
        
        cplex.setParam( IloCplex.Param.Threads, MAX_CPLEX_THREADS);
        cplex.clearCallbacks();
        cplex.use ( new SolveBranchcallback ( ) );
        cplex.setParam( IloCplex.Param.TimeLimit,  SOLUTION_CYCLE_TIME_SECONDS);
        logger.info (" MIP emphasis is " + MIP_EMPHASIS_TO_USE) ;
        logger.info (" LCA count threshold is " +  Parameters.COUNT_OF_LCA_NODES_TO_IDENTIFY) ;
        
        for (int cycles = ONE;  ; cycles ++){    
            
            cplex.solve();        
            bestBoundAchieved= cplex.getBestObjValue();
            if (cplex.getStatus().equals( IloCplex.Status.Feasible ) || cplex.getStatus().equals( IloCplex.Status.Optimal )) 
                bestSolutionFound =cplex.getObjValue();
            this.numNodesProcessed = cplex.getNnodes64();
                           
            log_statistics(  cycles );
            
            if (isCompletelySolved(cutoff)) {   
                
               
                end();
                break;
            }

        }         
          
    }
            
    public void prune (Set< IloCplex.NodeId >  migratedLeafs ) throws IloException{
        cplex.setParam( IloCplex.Param.Threads, ONE);
        cplex.clearCallbacks();
         
        cplex.use ( new PruneNodecallback  ( )) ;
        cplex.use ( new SolveBranchcallback( ));
        
        pruneSet.addAll(migratedLeafs );
        cplex.setParam( IloCplex.Param.TimeLimit,  BILLION);
        cplex.solve();
        pruneSet.clear(); //should already be empty
        
    }
     
    public void end () {
        if (!isCompletelySolved){
            cplex.end();
            isCompletelySolved= true;
        }        
    }
    
    protected boolean isCompletelySolved(double upperCutoff) throws IloException {
       
        boolean condition1 =  cplex.getStatus().equals( IloCplex.Status.Infeasible) || 
               cplex.getStatus().equals( IloCplex.Status.Optimal);
        boolean condition2 = false;
        
        double localCutoff = BILLION;
        if (upperCutoff < BILLION) localCutoff= upperCutoff;
        if (cplex.getStatus().equals( IloCplex.Status.Feasible)){
            double bestLocalSoln = cplex.getObjValue();
            if (bestLocalSoln < localCutoff){
                localCutoff = bestLocalSoln;
            }
        }
        
        if (localCutoff < BILLION){
            //|bestbound-upperCutoff|/(1e-10+|upperCutoff|) 
            double dist_mip_gap = Math.abs( cplex.getBestObjValue() - localCutoff );
            double denominator =  DOUBLE_ONE/ (BILLION) ;
            denominator = denominator /TEN;
            denominator = denominator +  Math.abs( localCutoff);
            dist_mip_gap = dist_mip_gap /denominator;
            condition2 = dist_mip_gap < EPSILON;
        }
        
         
        
        return condition2 || condition1;
    }
    
    protected void log_statistics (  int hour) throws IloException {
        double bestSoln = BILLION;
        double relativeMipGap = BILLION;
        IloCplex.Status cplexStatus  = cplex.getStatus();
        if (cplexStatus.equals( IloCplex.Status.Feasible)  ||cplexStatus.equals( IloCplex.Status.Optimal) ) {
            bestSoln=cplex.getObjValue();
            relativeMipGap=  cplex.getMIPRelativeGap();
        };
        logger.info("" + hour + ","+  bestSoln + ","+  
                cplex.getBestObjValue() + "," + cplex.getNnodesLeft64() +
                "," + cplex.getNnodes64() + "," + relativeMipGap ) ;
    }
    
      
    //return the complete tree structure used for tree traversal . 
    //Needed for implementing LCA and CB algorithms
    protected TreeStructureNode getTreeStructure () throws IloException{
        cplex.setParam( IloCplex.Param.Threads, ONE);
        cplex.setParam( IloCplex.Param.TimeLimit,  BILLION);
        cplex.clearCallbacks();
        LeafEnumerateNodecallback leafEnumCallback = new LeafEnumerateNodecallback () ;
        cplex.use (leafEnumCallback );
        cplex.use ( new SolveBranchcallback(  ) );
        cplex.solve();
        
        Set < TreeStructureNode> leafSet = leafEnumCallback.leafNodeAttahments;
        TreeStructureNode root = getTreeStructure (leafSet );
         
        
        return root;
    }
    
      
    protected  TreeStructureNode getTreeStructure (Set< TreeStructureNode> leafSet) throws IloException{
        
        
        
        
        TreeStructureNode root = null;
        Map <NodeAttachment , TreeStructureNode> mapOf_Attachment_to_StructureNodes = new HashMap <NodeAttachment , TreeStructureNode>();
        Set< TreeStructureNode> nonLeafNodesInThisRound = new HashSet< TreeStructureNode> ();
        
        for (TreeStructureNode leafNode : leafSet){
            TreeStructureNode parentTS = attach_TSNode_To_its_Parent (leafNode,  mapOf_Attachment_to_StructureNodes) ;
            nonLeafNodesInThisRound.add (parentTS);
        }
        
        Set<TreeStructureNode> nonleafNodes = new HashSet<TreeStructureNode>();
        nonleafNodes.addAll (nonLeafNodesInThisRound);
        nonLeafNodesInThisRound.clear();
        
        while (nonleafNodes.size()>ZERO){
            for (TreeStructureNode nonleafNode: nonleafNodes){
                if (nonleafNode.nodeAttachment.parentNode==null){
                   root =  nonleafNode;
                   root.leafSet = leafSet;
                   continue;
                }
                TreeStructureNode parentTS = attach_TSNode_To_its_Parent (nonleafNode,  mapOf_Attachment_to_StructureNodes) ;
                nonLeafNodesInThisRound.add (parentTS);
            }
            
            //prepare for next iteration
            nonleafNodes.clear();
            nonleafNodes.addAll (nonLeafNodesInThisRound);
            nonLeafNodesInThisRound.clear();     
        }                 
        
        return root;
    }
    
        
    //attach to parent and return parent
    protected TreeStructureNode attach_TSNode_To_its_Parent (TreeStructureNode node,
            Map <NodeAttachment , TreeStructureNode> map_Attachment_to_StructureNodes ) {
        
        //if my parent treeStructureNode does not exist, create it and insert into map
        TreeStructureNode parentTS = map_Attachment_to_StructureNodes.get( node.nodeAttachment.parentNode);
        if (null == parentTS){
            parentTS =     new        TreeStructureNode ()     ;
            parentTS.nodeAttachment =  node.nodeAttachment.parentNode;
            map_Attachment_to_StructureNodes.put( node.nodeAttachment.parentNode , parentTS);
        } 
        node.parent= parentTS;
        
        parentTS.lpRelaxObjective = Math.min (parentTS.lpRelaxObjective, node.lpRelaxObjective) ;

        //fill up parentTS
        if (node.nodeAttachment.am_I_The_Down_Branch_Child){
            parentTS.downBranchChild = node;            
        }else {
            parentTS.upBranchChild=node;            
        }
        
        return  parentTS;
                 
    }
    
}
