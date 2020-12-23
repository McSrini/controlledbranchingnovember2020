/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.controlledbranchingnovember2020.server;
 
import static ca.mcmaster.controlledbranchingnovember2020.Constants.*;
import ca.mcmaster.controlledbranchingnovember2020.client.*;
import ca.mcmaster.controlledbranchingnovember2020.subtree.lca.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 * 
 * public access added for test drivers, otherwise package local is enough
 */
public class Loadbalancer {
    
    private static HashMap <String,   TreeMap < Double, Integer> > availableLCANodes = 
            new HashMap <String,   TreeMap < Double, Integer>  > ();
    
     
        
    
    public static void balance (){
         
        availableLCANodes.clear();
        
        Set<String> setOfIdleClients = new HashSet<String>() ;
        
        for (Map.Entry<String, ClientRequestObject> entry : Server.map_Of_IncomingRequests.entrySet()){
            String clientName = entry.getKey();
            ClientRequestObject req = entry.getValue();
            if (req.isIdle) setOfIdleClients .add ( entry.getKey() );
            
            if (null!= req.availableLCANodes){
                TreeMap < Double, Integer> availabilityMap = availableLCANodes.get (clientName );
                if (null==availabilityMap) availabilityMap= new TreeMap < Double, Integer> ();
            
                for (Map.Entry < Double, ArrayList<Lite_LCA_Node>> nodeMapEntry : req.availableLCANodes.entrySet()){
                    availabilityMap.put(nodeMapEntry.getKey() , nodeMapEntry.getValue().size() );
                }            
                availableLCANodes.put (clientName ,availabilityMap );
            }
                   
        }
        
        for (String thisIdleClient: setOfIdleClients ){
            //get an LCA node with the best reamining LP relax 
            Tuple_StringDouble donorTuple = getDonor ( );
            if (donorTuple.name!=null){
                Lite_LCA_Node lcaLite = getLCA_fromDonor (donorTuple);
                makeLCAAssignment (thisIdleClient, lcaLite );
                notePruneInstruction (donorTuple.name, lcaLite.numLeafsRepresented);
                System.out.println("donor "+donorTuple.name + " will prune leaf count" +   lcaLite.numLeafsRepresented) ;
            }
            
        }
        
    }
    
    private static void  notePruneInstruction (String donor, double lpRelax ){
        ServerResponseObject resp  =Server.responseMap.get( donor);
        if (null == resp.pruneList)    resp.pruneList =   new TreeMap < Double, Integer> ();
        Integer count = resp.pruneList.get(lpRelax) ;
        if (null == count) count = ZERO;
        resp.pruneList.put(lpRelax, count+ONE) ;
    }
    
    private static void  makeLCAAssignment (String thisIdleClient,Lite_LCA_Node lcaLite ){
        ServerResponseObject resp  =Server.responseMap.get( thisIdleClient);
        resp.assignment = lcaLite;
    }
    
    private static Lite_LCA_Node getLCA_fromDonor (Tuple_StringDouble donorTuple){
        Lite_LCA_Node result = null;
        ArrayList<Lite_LCA_Node> thisArrayList = Server.map_Of_IncomingRequests.get(donorTuple.name).availableLCANodes.get(donorTuple.value);
        result =thisArrayList .remove( thisArrayList.size()-ONE);
        //if (ZERO == thisArrayList.size())  Server.map_Of_IncomingRequests.get(donor).availableLCANodes.remove (entry.getKey());
        //if (ZERO==Server.map_Of_IncomingRequests.get(donor).availableLCANodes.size())             Server.map_Of_IncomingRequests.get(donor).availableLCANodes = null;
        return result;
    }
    
    private static Tuple_StringDouble getDonor (){
        String donor = null;
        double lowestKnown = BILLION;
        for (Map.Entry <String,   TreeMap < Double, Integer> > entry : availableLCANodes.entrySet()){
            if (entry.getValue().firstKey() < lowestKnown){
                lowestKnown = entry.getValue().firstKey() ;
                donor = entry.getKey();
            }
        }
        if (null!=donor){
            int newValue = -ONE + availableLCANodes.get(donor).remove(lowestKnown );
            if (newValue > ZERO) {
                availableLCANodes.get(donor).put(lowestKnown ,newValue );
            }else {
                if (availableLCANodes.get(donor).size()==ZERO){
                    availableLCANodes.remove(donor);
                }
            }
        }
        return new Tuple_StringDouble(donor, lowestKnown);
    }
    
}
