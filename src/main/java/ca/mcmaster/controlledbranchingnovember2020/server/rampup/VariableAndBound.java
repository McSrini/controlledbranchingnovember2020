/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.controlledbranchingnovember2020.server.rampup;
 
import static ca.mcmaster.controlledbranchingnovember2020.Constants.BILLION;
import ilog.concert.IloNumVar;

/**
 *
 * @author tamvadss
 */
class VariableAndBound {
    
    private IloNumVar variable=null ;
    private double bound = BILLION;
   
    public VariableAndBound (IloNumVar var  ,    double var_bound   ){
        this .variable = var ;
        this .bound=var_bound;
    }
    
    public IloNumVar getVar (){
        return variable;
    }
    
    public double getBound (){
        return bound;
    }
}
