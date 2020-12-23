/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.controlledbranchingnovember2020.drivers;
 
import static ca.mcmaster.controlledbranchingnovember2020.Constants.ZERO;
import ca.mcmaster.controlledbranchingnovember2020.client.Client;
import ca.mcmaster.controlledbranchingnovember2020.client.ClientRequestObject;
import ca.mcmaster.controlledbranchingnovember2020.server.Loadbalancer;
import ca.mcmaster.controlledbranchingnovember2020.server.Server;
import ca.mcmaster.controlledbranchingnovember2020.server.ServerResponseObject;
import ca.mcmaster.controlledbranchingnovember2020.server.rampup.RampUp;
import ca.mcmaster.controlledbranchingnovember2020.subtree.lca.Lite_LCA_Node;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 */
public class ClientServer_testDriver {
    
    public static void main(String[] args) throws Exception {
        RampUp ramp_up = new RampUp();
        List<Lite_LCA_Node>       leafPool_FromRampUp = ramp_up.doRampUp ();
        double    globalIncombent = ramp_up.getSolutionValue();
            
        for (Lite_LCA_Node lite:       leafPool_FromRampUp)    {
            //lite.printMe();
        }
        
        
        //Client.test_hook();
        
        testServer();
            
    }
    
    public static void testServer() {
        
        Server server = new Server ();
        server.map_Of_IncomingRequests  = new HashMap < String, ClientRequestObject >   ();
        
        ClientRequestObject cro1= new ClientRequestObject ();
        server.map_Of_IncomingRequests.put ("client1", cro1);
        ClientRequestObject cro2= new ClientRequestObject ();
        server.map_Of_IncomingRequests.put ("client2", cro2);
        ClientRequestObject cro3= new ClientRequestObject ();
        server.map_Of_IncomingRequests.put ("client3", cro3);
        ClientRequestObject cro4= new ClientRequestObject ();
        server. map_Of_IncomingRequests.put ("client4", cro4);
        ClientRequestObject cro5= new ClientRequestObject ();
        server.map_Of_IncomingRequests.put ("client5", cro5);
        
        cro1.isIdle = false;
        cro1. clientName="client1";
        cro1.local_bestBound =1;
        cro1. local_incumbent =25;
       
        cro1. availableLCANodes= new TreeMap < Double, ArrayList<Lite_LCA_Node>>();
         
        Lite_LCA_Node n11 = new Lite_LCA_Node ( );
        ArrayList<Lite_LCA_Node> l1 = new ArrayList<Lite_LCA_Node> ();
        l1.add (n11) ;
        Lite_LCA_Node n12 = new Lite_LCA_Node ( );
        ArrayList<Lite_LCA_Node> l2 = new ArrayList<Lite_LCA_Node> ();
        l2.add (n12) ;
        l1.addAll(l2);
        cro1. availableLCANodes.put (1.0,  l1  );
        n11.myID=11;
        n12.myID=12;
        n11.numLeafsRepresented= 1;
        n12.numLeafsRepresented= 1;
        
        cro2.isIdle = false;
        cro2. clientName="client2";
        cro2.local_bestBound =0;
        cro2. local_incumbent =26;
        cro2. availableLCANodes=    new TreeMap < Double, ArrayList<Lite_LCA_Node>>();
        ////////
        Lite_LCA_Node n21 = new Lite_LCA_Node ();
        ArrayList<Lite_LCA_Node> L1 = new ArrayList<Lite_LCA_Node> ();
        L1.add (n21) ;
        Lite_LCA_Node n22 = new Lite_LCA_Node ();
        ArrayList<Lite_LCA_Node> L2 = new ArrayList<Lite_LCA_Node> ();
        L2.add (n22) ;
        L1.addAll(L2 );
        cro2. availableLCANodes.put (2.0,  L1  );
        n21.myID = 21;
        n22.myID =22;
        n21.numLeafsRepresented = 2;
        n22.numLeafsRepresented = 2;
        
        cro3.isIdle = true;
        cro3. clientName="client3";
        cro3. availableLCANodes= null;
        
        cro4.isIdle = true;
        cro4. clientName="client4";
        cro4. availableLCANodes= null;
        
        cro5.isIdle = true;
        cro5. clientName="client5";
        cro5. availableLCANodes= null;
        
         
        Server.map_Of_IncomingRequests.put ("client1", cro1) ;
        Server.map_Of_IncomingRequests.put ("client2", cro2) ;
        Server.map_Of_IncomingRequests.put ("client3", cro3) ;
        Server.map_Of_IncomingRequests.put ("client4", cro4) ;
        Server.map_Of_IncomingRequests.put ("client5", cro5) ;

        for (Map.Entry<String, ClientRequestObject> entry : Server.map_Of_IncomingRequests.entrySet()){
            ServerResponseObject resp = new ServerResponseObject () ;  
            resp.globalIncumbent = Server.globalIncombent;                        
            Server.responseMap.put( entry.getKey(), resp);
        }

        Loadbalancer.balance ();
        for ( Map .Entry< String, ServerResponseObject > entry : Server.responseMap.entrySet() ){
            System.out.println("Response to "+ entry.getKey()) ;
            if (entry.getValue().assignment!=null){
                System.out.println(" Id " + entry.getValue().assignment.myID + " size "+ entry.getValue().assignment.numLeafsRepresented);
            }else {
                System.out.println("no assignment");
            }
            if (entry.getValue().pruneList!=null && entry.getValue().pruneList.size()> ZERO){
                System.out.println("  prunelist size "+ entry.getValue().pruneList.size()) ;
            }else System.out.println("no prunelist") ;
            
        }
         
    }

}
