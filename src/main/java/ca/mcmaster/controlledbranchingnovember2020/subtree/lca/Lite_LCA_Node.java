/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.controlledbranchingnovember2020.subtree.lca;

import static ca.mcmaster.controlledbranchingnovember2020.Constants.ZERO;
import ca.mcmaster.controlledbranchingnovember2020.subtree.BranchingOverrule;
import ca.mcmaster.controlledbranchingnovember2020.subtree.Lite_VariableAndBound;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author tamvadss
 */
public class Lite_LCA_Node {
    
    private static int START_ID = ZERO;
    
     
    public int myID;
    public double lpRelax;
    public long numLeafsRepresented ;
    
    //bound and isUpperBound ?
    public HashMap<Lite_VariableAndBound , Boolean> varFixings = new HashMap<Lite_VariableAndBound , Boolean>();
    
    //my branching instructions, and the instruction I need to pass on to both kids
    public BranchingOverrule branchingOverrule;
    
    public Lite_LCA_Node (){
        myID = ++ START_ID;
    }
    
    public void printMe () {
        System.out.println("\n\n\n myID "+ myID + " lpRelax "+ lpRelax+ " numLeafsRepresented "+ numLeafsRepresented) ;
        System.out.println("printing var fixings ") ;
        for (Map.Entry<Lite_VariableAndBound , Boolean> entry : varFixings.entrySet() ){
            System.out.println("var " +entry.getKey().varName + " bound " + entry.getKey().bound + " dir "+ entry.getValue()) ;
        }
        branchingOverrule.printMe();
    }
        
}
