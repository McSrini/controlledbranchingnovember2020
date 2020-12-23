/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.controlledbranchingnovember2020.server;
 
import static ca.mcmaster.controlledbranchingnovember2020.Constants.*;
import static ca.mcmaster.controlledbranchingnovember2020.Parameters.*;
import ca.mcmaster.controlledbranchingnovember2020.client.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import static java.lang.System.exit;
import java.net.Socket;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class RequestHandler implements Runnable{
    
    private Socket clientSocket;
        
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RequestHandler .class); 
    private static RollingFileAppender rfa  = null;
        
    static {
        logger.setLevel( LOGGING_LEVEL);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            rfa =new  RollingFileAppender(layout,LOG_FOLDER+  RequestHandler.class.getSimpleName()+ LOG_FILE_EXTENSION);
            rfa.setMaxBackupIndex(SIXTY);
            rfa.setImmediateFlush(true);
            logger.addAppender(rfa);
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging"+ex);       
            exit(ONE);
        }
        
       
    } 
    public RequestHandler ( Socket clientSocket ){        
        this.clientSocket = clientSocket;       
    }
 
    @Override
    public void run() {
        try (               
                ObjectOutputStream  outputStream = new ObjectOutputStream( clientSocket.getOutputStream());
                ObjectInputStream inputStream =  new ObjectInputStream(clientSocket .getInputStream());                 
            ){
            
            ClientRequestObject requestFromClient =   (ClientRequestObject) inputStream.readObject() ;
            logger.debug (" request recieved from client " + requestFromClient  ) ;           
            
            while (true){          
                //note the request in our synchronized map
                Server.map_Of_IncomingRequests.put( requestFromClient.clientName , requestFromClient);
               
                //process request and prepare response
                //this includes updating the best known solution to the server 
                ServerResponseObject resp = prepareResponse (   requestFromClient );
                
                //send response to client
                outputStream.writeObject(resp);
                
                 
                
                //read the next request from the same client
                requestFromClient =   (ClientRequestObject) inputStream.readObject() ;
                logger.debug (" request recieved from client " + requestFromClient ) ;           
            }
                        
            
        }catch (Exception ex){
            ex.printStackTrace();
            System.err.println(ex.getMessage());
            exit(ONE);
        }finally {
            try {
                clientSocket.close();
            }catch (IOException ioex){
                System.err.println(ioex );
                exit(ONE);
            }
        }
    }

    
    private ServerResponseObject prepareResponse (  ClientRequestObject requestFromClient ) throws Exception{
                
        //first wait for all requests to come in
        if (! waitForAllClients()){
            throw new Exception ("ERROR: Request not recieved from all clients !") ;
        }
        
        //the first thread that notices an empty response map prepares all the responses
        //if you see a full response map, just skip 
        populateResponseMap ( );
         
        
        //remove this client's response from the response map , and return it  
        //
        //the last thread to send its response must also clear the request map, in preparation for
        //the next solution cycle
        ServerResponseObject resp = null;
        synchronized ( Server.responseMap) {
            resp = Server.responseMap.remove( requestFromClient.clientName);         
            if (Server.responseMap.isEmpty()){
                Server.map_Of_IncomingRequests.clear();
            }
        }
        
         
        
        return resp;
        
    }
    
    private void populateResponseMap () throws Exception{
        
        synchronized ( Server.responseMap) {
            if (Server.responseMap.isEmpty()){
                //populate it
                
                int numIdleClients  = ZERO; 
                Server.lowestBoundOfAllClients = BILLION;
                
                for (Map.Entry<String, ClientRequestObject> entry : Server.map_Of_IncomingRequests.entrySet()){
                    Server.globalIncombent = Math.min (Server.globalIncombent, entry.getValue().local_incumbent) ;     
                    Server.lowestBoundOfAllClients= Math.min (Server.lowestBoundOfAllClients, entry.getValue().local_bestBound );
                    if (entry.getValue().isIdle) numIdleClients++;
                    Server.numNodesProcessed_Total += entry.getValue().numNodesProcessed;
                }
                
               
                
                logger.info ("" + (Server.iterationNumber  ++ ) + " ,Best solution and bound so far are , " + Server.globalIncombent + " , " + Server.lowestBoundOfAllClients);
                
                boolean areAllworkersHaveCompletedTheirAssignment =  
                        (numIdleClients==NUM_WORKERS && Server.leafPool_FromRampUp.size()==ZERO);
                        
                boolean doLoadBalance = true;
                if (numIdleClients<NUM_WORKERS && numIdleClients> ZERO && Server.globalIncombent<= Server.lowestBoundOfAllClients ){
                    //some worker has found the optimal in best first search, while other workers are doing non critucal work
                    doLoadBalance = false;                    
                }   
                if ( isWithinDistMipGap (  Server.lowestBoundOfAllClients,   Server.globalIncombent)){
                    // do not load balance, just inform all workers of the global incumbent and the computation will end
                    doLoadBalance = false;    
                }
                
                
                boolean isJustAfterRampup = numIdleClients==NUM_WORKERS && Server.leafPool_FromRampUp!=null &&
                        Server.leafPool_FromRampUp.size() == NUM_WORKERS;
                
                if (areAllworkersHaveCompletedTheirAssignment  ) {
                    
                    logger.info ("All workers are complete! "+ " total nodes processed is "+  Server.numNodesProcessed_Total );
                    
                    exit(ZERO);
                    
                    //workers are never sent a halt instruction
                    
                    //complete
                    for (Map.Entry<String, ClientRequestObject> entry : Server.map_Of_IncomingRequests.entrySet()){
                        ServerResponseObject resp = new ServerResponseObject () ;  
                        resp.haltFlag= true;                        
                        Server.responseMap.put( entry.getKey(), resp);
                    }
                    
                }else if (isJustAfterRampup){
                    //just after ramp up
                    for (Map.Entry<String, ClientRequestObject> entry : Server.map_Of_IncomingRequests.entrySet()){
                        ServerResponseObject resp = new ServerResponseObject () ;  
                        resp.assignment = Server.leafPool_FromRampUp.remove(ZERO);
                        resp.globalIncumbent = Server.globalIncombent;
                        Server.responseMap.put( entry.getKey(), resp);
                    }
                    
                    logger.info ("ramp up response complete" );
                    
                    if (Server.leafPool_FromRampUp.size()!=ZERO){
                        //error , should not happen
                        throw new Exception ("Ramp up was loo large ! " );
                    }
                    
                }else if (numIdleClients==ZERO){
                    //  just run another solution cycle
                    for (Map.Entry<String, ClientRequestObject> entry : Server.map_Of_IncomingRequests.entrySet()){
                        ServerResponseObject resp = new ServerResponseObject () ;  
                        resp.globalIncumbent = Server.globalIncombent;
                        Server.responseMap.put( entry.getKey(), resp);
                    }
                    
                }else {
                    
                    for (Map.Entry<String, ClientRequestObject> entry : Server.map_Of_IncomingRequests.entrySet()){
                        ServerResponseObject resp = new ServerResponseObject () ;  
                        resp.globalIncumbent = Server.globalIncombent;                        
                        Server.responseMap.put( entry.getKey(), resp);
                    }
                    //load balance
                    //take the lowest lp relax LCA node, and 
                    //assign to an idle client, until 
                    //no more idle clients or no more LCA available
                    if (doLoadBalance) Loadbalancer.balance();
                    
                }
                
            }//if empty response map
        }//sync
        
    }//method populate response map
    
 
           
    private boolean waitForAllClients () throws InterruptedException{
        
        boolean result = false;
        
        for  (int limit = ZERO; limit < SIXTY * TWO*TWO * SIXTY; limit ++ ) {
            
            int countOfRecieved = ZERO;
            
            synchronized ( Server.map_Of_IncomingRequests) {
               countOfRecieved = Server.map_Of_IncomingRequests.size();
            }//synch
            
            if ( countOfRecieved <  NUM_WORKERS){
                //sleep for a second
                Thread.sleep( THOUSAND);
            }else {
                result = true;
                break;
            }
            
           
        }//end for limit
        
        return result;
    }
    
    private boolean isWithinDistMipGap (double bound, double upperCutoff) {        
        double dist_mip_gap = Math.abs(  bound - upperCutoff);
        double denominator =  DOUBLE_ONE/ (BILLION) ;
        denominator = denominator /TEN;
        denominator = denominator +  Math.abs(upperCutoff);
        dist_mip_gap = dist_mip_gap /denominator;
        logger.info ("dist_mip_gap is " + dist_mip_gap + " " + EPSILON) ;
        return dist_mip_gap < EPSILON;
    }
        
}
