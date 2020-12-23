/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.controlledbranchingnovember2020.server;

import static ca.mcmaster.controlledbranchingnovember2020.Constants.*;
import ca.mcmaster.controlledbranchingnovember2020.subtree.Lite_VariableAndBound;
import ca.mcmaster.controlledbranchingnovember2020.subtree.lca.Lite_LCA_Node;
import java.io.Serializable;
import java.util.HashMap;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 */
public class ServerResponseObject  implements Serializable {
        
    public double globalIncumbent = BILLION;
    public boolean haltFlag =false;
    public  Lite_LCA_Node assignment=null ;
    
    
    
    //how many LCA nodes at each LP relax value were distributed to other workers?
    //note , LCA nodes are chosen starting the end of the available list, for each lprelax
    public TreeMap < Double, Integer> pruneList=null;
}
