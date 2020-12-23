/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.controlledbranchingnovember2020.client;

import static ca.mcmaster.controlledbranchingnovember2020.Constants.*;
import ca.mcmaster.controlledbranchingnovember2020.Parameters;
import static ca.mcmaster.controlledbranchingnovember2020.Parameters.*;
import ca.mcmaster.controlledbranchingnovember2020.server.ServerResponseObject;
import ca.mcmaster.controlledbranchingnovember2020.subtree.BranchingOverrule;
import ca.mcmaster.controlledbranchingnovember2020.subtree.Lite_VariableAndBound;
import ca.mcmaster.controlledbranchingnovember2020.subtree.TreeStructureNode;
import ca.mcmaster.controlledbranchingnovember2020.subtree.lca.*;
import ca.mcmaster.controlledbranchingnovember2020.utils.LCA_Utils;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import static java.lang.System.exit;
import static java.lang.Thread.sleep;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class Client {
    
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Client.class); 
    private static   String clientname =null;
    
    //used for communication to server
    private static TreeMap < Long, ArrayList<TreeStructureNode >> map_of_collected_LCA_Nodes = 
            new TreeMap < Long, ArrayList< TreeStructureNode>> ();
    
    //job I will work on, null to begin
    private static Subtree_LCA mySubTree= null;
    
    private static boolean willBeIdleForWholeCycle = false;
    
    static {
        logger.setLevel( LOGGING_LEVEL);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa =new  RollingFileAppender(layout,LOG_FOLDER+ Client.class.getSimpleName()+ LOG_FILE_EXTENSION);
            rfa.setMaxBackupIndex(SIXTY);
            logger.addAppender(rfa);
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging"+ex);       
            exit(ONE);
        }
    } 
    
    public static void main(String[] args) throws Exception {
        
        clientname =  InetAddress.getLocalHost(). getHostName() ;
        
        try (
            Socket workerSocket = new Socket(SERVER_NAME, SERVER_PORT_NUMBER);                
            ObjectOutputStream  outputStream = new ObjectOutputStream(workerSocket.getOutputStream());
            ObjectInputStream inputStream =  new ObjectInputStream(workerSocket.getInputStream());
            
        ){
            
            logger.info ("Client is starting ... "+ clientname) ;
            logger.info ("COUNT_OF_LCA_NODES_TO_IDENTIFY " + Parameters.COUNT_OF_LCA_NODES_TO_IDENTIFY );
            logger.info( "presolve config " + Parameters.DISABLE_PRESOLVE + " " + Parameters.DISABLE_PRESOLVE_NODE);
           
            for (int iteration = ZERO ; iteration <  MAX_SOLUTION_CYCLES ; iteration  ++){
                
                //get work from server
                ClientRequestObject request = prepareRequest(iteration) ;
                logger.info (" sending request "   );
                outputStream.writeObject(request);
               
                ServerResponseObject response = (ServerResponseObject) inputStream.readObject();
                
                
                if (  response.haltFlag) break;
                
                long  startTime = System.currentTimeMillis();            
                 
                long pruneTime_milliSeconds = processResponse(response) ;
                
                logger.info ("processresponse and prune took seconds" + pruneTime_milliSeconds/THOUSAND);
                
                //solve for SOLUTION_CYCLE_TIME
                    
                if (!mySubTree.isCompletelySolved) mySubTree.solve(response.globalIncumbent,pruneTime_milliSeconds );
                long idleTime = THOUSAND * SOLUTION_CYCLE_TIME_SECONDS  - (System.currentTimeMillis() -startTime);
                if ( idleTime > ZERO) {
                    logger.info ("Client "+ clientname + " will be idle for  sec " + idleTime/THOUSAND);
                    sleep(idleTime) ;
                } else {
                    logger.info ("solve completed");
                }
                            
                           
            }//end for iterations
            
            
             
            workerSocket.close();
            logger.info ("Client is stopping ... "+ clientname) ;
             
        } catch (Exception ex) {
             System.err.println(ex);
             ex.printStackTrace();
        }
        
    }
    
    public static void test_hook() throws IloException {
        //prepare avail LCA nodes map and process reponse by printing prune list
        
        mySubTree= new Subtree_LCA (  new HashMap<Lite_VariableAndBound , Boolean>() , 0 , null);
        
        //solve for some time
        mySubTree.solve(BILLION, SOLUTION_CYCLE_TIME_SECONDS*THOUSAND -44100);
        
        mySubTree.root.printMe();
        
        map_of_collected_LCA_Nodes = mySubTree.collectLCANodes();
        for (Map.Entry < Long, ArrayList<TreeStructureNode >> entry : map_of_collected_LCA_Nodes.entrySet()){
            System.out.println("key is "+ entry.getKey() );
            for (TreeStructureNode tsn : entry.getValue()){
                Set < IloCplex.NodeId> pruneTragets = mySubTree.getLeafSetForLCANode (tsn) ;
                System.out.println("prune targets for this tsn are ");
                for (IloCplex.NodeId ids: pruneTragets){
                    System.out.println("" + ids) ;
                }
            }
        }
        
        ClientRequestObject req = new ClientRequestObject ();
        req.availableLCANodes = getMapOfLiteLCANodes (map_of_collected_LCA_Nodes);
        
        ServerResponseObject responseFromServer = new ServerResponseObject ();
        responseFromServer.pruneList= new TreeMap < Double, Integer> () ;
        
        System.out.println("printing available LCA nodes");
        for (Map.Entry <Double, ArrayList<Lite_LCA_Node>> entry : req.availableLCANodes.entrySet()){
            System.out.println(entry.getKey() + " size " + entry.getValue().size()) ;
            responseFromServer.pruneList.put (entry.getKey(),   ONE );
        }
        
        System.out.println("\n\n printing prune target  nodes\n\n");
        
        for (IloCplex.NodeId ids : getNodeIDs_ForPruning (responseFromServer.pruneList)){
            System.out.println("prune node ids: " + ids.toString() ) ;    
        }
        
        System.out.println("Client test complete") ;
        
    }
    
    private static    ClientRequestObject  prepareRequest( int iteration) {
        ClientRequestObject req = new ClientRequestObject ();
        req.clientName=clientname;
        if (ZERO==iteration) {
            //just starting, get assignment of ramp up
            req.isIdle= true;            
        }else {
            if (mySubTree.isCompletelySolved){
                req.isIdle = true;
                req.local_bestBound = mySubTree.bestBoundAchieved;
                req.local_incumbent= mySubTree.bestSolutionFound;
                if (! willBeIdleForWholeCycle) req.numNodesProcessed= mySubTree.numNodesProcessed;
            }else {
                req.isIdle = false;
                req.local_bestBound = mySubTree.bestBoundAchieved;
                req.local_incumbent= mySubTree.bestSolutionFound;
                //req.numNodesProcessed= mySubTree.numNodesProcessed;
                //get the perfect LCA nodes
                map_of_collected_LCA_Nodes=  mySubTree.collectLCANodes();
                if (  map_of_collected_LCA_Nodes.size()>ZERO){
                    req.availableLCANodes = getMapOfLiteLCANodes (map_of_collected_LCA_Nodes);
                }                
            }
        }
        return req;
    }
    
    private static TreeMap < Double, ArrayList<Lite_LCA_Node>> getMapOfLiteLCANodes (
        TreeMap    < Long , ArrayList< TreeStructureNode>  > inputMap){
        
        TreeMap < Double, ArrayList<Lite_LCA_Node>> result = new TreeMap < Double, ArrayList<Lite_LCA_Node>> ();
        
        for (Map.Entry    < Long , ArrayList< TreeStructureNode>  > entry : inputMap.entrySet()){
            Double key = DOUBLE_ZERO+ entry.getKey();
            ArrayList<Lite_LCA_Node> thisArry = new  ArrayList<Lite_LCA_Node> ();
            for (TreeStructureNode tsNode: entry.getValue()){
                thisArry.add(LCA_Utils.convertToLiteLCA(tsNode, mySubTree.myRoot_VarFixings) );
            }
            if (thisArry.size()>ZERO) result.put (key, thisArry);
        }
        
        return result;
        
    }
    
      
    private static long  processResponse(ServerResponseObject responseFromServer ) throws IloException {
        logger.info(" processing Response " );
        
        long result = ZERO;
        willBeIdleForWholeCycle =false;
        
        if (null != responseFromServer.pruneList){
            //prune leafs for LCA nodes that were migrated to other workers   
            long  prune_startTime = System.currentTimeMillis();
            mySubTree.prune(getNodeIDs_ForPruning (responseFromServer.pruneList));
            result = System.currentTimeMillis() - prune_startTime;
            
        }
        if (null!=responseFromServer.assignment){
            //
            logger.info(" creating SubTree_LCA ... " );
            int  iterationsCompleted = mySubTree == null ? ZERO: mySubTree.iterationsCompleted;
            mySubTree = new Subtree_LCA ( responseFromServer.assignment.varFixings , iterationsCompleted, 
                                          responseFromServer.assignment.branchingOverrule);
            logger.info(" SubTree_LCA created " );
            
        }else if (mySubTree.isCompletelySolved){
            logger.warn ("idle worker got no assignent") ;
            willBeIdleForWholeCycle= true;
        }
        return result;
    }
    
    private static Set < IloCplex.NodeId> getNodeIDs_ForPruning (TreeMap < Double, Integer> pruneList){
        Set < IloCplex.NodeId> pruneTargets  = new HashSet < IloCplex.NodeId> () ;
        
        for (Map.Entry < Double, Integer> entry : pruneList.entrySet()){
            Long key = Math.round( entry.getKey());
            int count = entry.getValue();
            while  ( count > ZERO ){
                //remove last entry
                TreeStructureNode tsNode  =
                        map_of_collected_LCA_Nodes.get(key).remove( -ONE + map_of_collected_LCA_Nodes.get(key).size()) ;
                pruneTargets.addAll (mySubTree.getLeafSetForLCANode(tsNode)) ;
                count --;
            }
        }
        
        return pruneTargets;
        
    }
    
}
