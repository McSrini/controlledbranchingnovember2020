/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.controlledbranchingnovember2020.server;
  
import static ca.mcmaster.controlledbranchingnovember2020.Constants.*;
import static ca.mcmaster.controlledbranchingnovember2020.Parameters.*;
import ca.mcmaster.controlledbranchingnovember2020.client.*;
import ca.mcmaster.controlledbranchingnovember2020.server.rampup.*;
import ca.mcmaster.controlledbranchingnovember2020.subtree.lca.*;
import static java.lang.System.exit;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class Server {
        
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Server .class); 
    
    //key is client name
    public static Map < String, ClientRequestObject >   map_Of_IncomingRequests   = Collections.synchronizedMap(new HashMap< String, ClientRequestObject > ()); 
    public static Map < String, ServerResponseObject >   responseMap  = Collections.synchronizedMap(new HashMap< String, ServerResponseObject > ()); 
            
    public static double globalIncombent = BILLION;
    public static double lowestBoundOfAllClients = BILLION;
    public static long   numNodesProcessed_Total = ZERO ;
    //public static double relativeMipGap = BILLION;
    public static List<Lite_LCA_Node> leafPool_FromRampUp =new ArrayList<Lite_LCA_Node> ();
    public static int iterationNumber = ZERO;
    
    static {
        logger.setLevel( LOGGING_LEVEL);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa =new  RollingFileAppender(layout,LOG_FOLDER+  Server.class.getSimpleName()+ LOG_FILE_EXTENSION);
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
        RampUp ramp_up = new RampUp();
        leafPool_FromRampUp= ramp_up.doRampUp ();
        globalIncombent = ramp_up.getSolutionValue();
        
        ExecutorService executor = null;
        try (
                //try with resources 
                ServerSocket serverSocket = new ServerSocket(SERVER_PORT_NUMBER);               
                
            ) {
            String hostname =  InetAddress.getLocalHost(). getHostName() ;
            System.out.println("The   server is running..." + hostname);
            
            executor = Executors.newFixedThreadPool(  NUM_WORKERS );     
            
            while (true ){
                Socket clientSocket = serverSocket.accept();
                RequestHandler requestHandler = new RequestHandler(clientSocket) ;
                executor.execute(requestHandler);                  
            }
            
        } catch(Exception ex) {
            ex.printStackTrace();
            System.err.println(ex.getMessage());
        }finally{
            if (executor!=null){
                executor.shutdown();
            }
        }
        
    }
    
}
