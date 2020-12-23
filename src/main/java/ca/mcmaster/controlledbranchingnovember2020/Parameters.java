/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.controlledbranchingnovember2020;

import static ca.mcmaster.controlledbranchingnovember2020.Constants.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author tamvadss
 */
public class Parameters {
    
       
    //public static final String PRESOLVED_MIP_FILENAME = "tutaki.pre.sav";  
    public static final String PRESOLVED_MIP_FILENAME = "mip.pre.sav";  
    //public static final String PRESOLVED_MIP_FILENAME = "F:\\temporary files here recovered\\bnatt500.pre.sav";  
        
    public static  int MAX_CPLEX_THREADS =   System.getProperty("os.name").toLowerCase().contains("win") ? 1 : 32;
    public static final int FILE_STRATEGY_DISK_COMPRESSED= 3;    
    public static boolean DISABLE_HEURISTICS= true;
    public static final int MIP_EMPHASIS_TO_USE = 2;
    
    public static final  int NUM_WORKERS= 5;
    public static final String SERVER_NAME = "miscan-head";
    public static final int SERVER_PORT_NUMBER = 4444;
    public static final int SOLUTION_CYCLE_TIME_SECONDS= 5*360;    //half hour
    public static final int MAX_SOLUTION_CYCLES = BILLION;
    public static final int COUNT_OF_LCA_NODES_TO_IDENTIFY = 8;
    
   
   
    //
    public static final boolean DISABLE_PRESOLVE = false;
    public static final boolean DISABLE_PRESOLVE_NODE = false;   
    
      
    
}
