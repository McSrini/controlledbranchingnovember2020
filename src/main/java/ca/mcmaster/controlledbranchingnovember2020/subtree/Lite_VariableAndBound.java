/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.controlledbranchingnovember2020.subtree;

import java.io.Serializable;

/**
 *
 * @author tamvadss
 */
public class Lite_VariableAndBound implements Serializable {
    
    public String varName;
    public double bound;
    
    public Lite_VariableAndBound (){
        
    }
    
    public Lite_VariableAndBound ( String varName,double bound) {
        this. varName= varName;
        this. bound= bound;
    }
    
}
