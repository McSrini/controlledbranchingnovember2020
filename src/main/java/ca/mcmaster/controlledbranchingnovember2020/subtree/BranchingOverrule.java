/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.controlledbranchingnovember2020.subtree;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author tamvadss
 */
public class BranchingOverrule implements Serializable {
    // var bound and isupperbound
    public  HashMap<Lite_VariableAndBound, Boolean> downBranchCondition = null;
    public HashMap<Lite_VariableAndBound, Boolean> upBranchCondition = null;
    
    public BranchingOverrule downChildsBranchingOverrule = null;
    public BranchingOverrule upChildsBranchingOverrule = null;
    
    //toString()
    public void printMe () {
        System.out.println("\n\nprinting BranchingOverrule " ); 
        System.out.println("printing down Branchin condition " ); 
        if ( downBranchCondition!=null){
            for (Map.Entry<Lite_VariableAndBound , Boolean> entry : downBranchCondition.entrySet() ){
                System.out.println("var " +entry.getKey().varName + " bound " + entry.getKey().bound + " dir "+ entry.getValue()) ;
            }
        }
        System.out.flush();
        System.out.println("printing up Branchin condition " ); 
        if (upBranchCondition!=null){
            for (Map.Entry<Lite_VariableAndBound , Boolean> entry : upBranchCondition.entrySet() ){
                System.out.println("var " +entry.getKey().varName + " bound " + entry.getKey().bound + " dir "+ entry.getValue()) ;
            }
        }

        System.out.flush();
        System.out.println("printing down childs Branchin overrule " ); 
        
        if (downChildsBranchingOverrule!=null) downChildsBranchingOverrule.printMe();
        
        System.out.flush();
        System.out.println("printing up childs Branchin overrule " ); 
        
        if (upChildsBranchingOverrule!=null)  upChildsBranchingOverrule.printMe();
        System.out.flush();
    }
    
}
