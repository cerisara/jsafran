/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jsafran.parsing.unsup;

import java.util.HashMap;

/**
 * Save the order in which the primary roles can appear wrt the predicate
    * where primary roles are(in a first version): A0, A1
    * so it can return one of the following orders
    * A0 A1 P, A0 P A1, P A0 A1,
    * A1 A0 P, A1 P A0, P A1 A0,
    * A0 P, P A0,
    * A1 P, P A1
 *
 * @author ale
 */
public class OrderRoles {
    HashMap<Integer,String> orders= new HashMap<Integer,String>();
    HashMap<String,Integer> invOrders= new HashMap<String,Integer>();
    int numOrders;
    public OrderRoles() {
        //load the orders dictionaries
        orders.put(17,"N"); //for none when it's not a predicate
        orders.put(0, "A0-A1-P");
        orders.put(1, "A0-P-A1");
        orders.put(2, "P-A0-A1");
        orders.put(3, "A1-A0-P");
        orders.put(4, "A1-P-A0");
        orders.put(5, "P-A1-A0");
        orders.put(6, "A0-P");
        orders.put(7, "P-A0");
        orders.put(8, "A1-P");
        orders.put(9, "P-A1");
        orders.put(10, "P");
        //see add the same role repeated???
        orders.put(11, "A1-A1-P");
        orders.put(12, "A1-P-A1");
        orders.put(13, "P-A1-A1");
        orders.put(14, "A0-A0-P");
        orders.put(15, "A0-P-A0");
        orders.put(16, "P-A0-A0");

        numOrders=orders.size(); //TODO, fixed now, but should depend on the number of roles to consider, see equation
        invOrders.put("N",17);
        invOrders.put( "A0-A1-P",0);
        invOrders.put( "A0-P-A1",1);
        invOrders.put( "P-A0-A1",2);
        invOrders.put( "A1-A0-P",3);
        invOrders.put("A1-P-A0",4);
        invOrders.put( "P-A1-A0",5);
        invOrders.put( "A0-P",6);
        invOrders.put( "P-A0",7);
        invOrders.put( "A1-P",8);
        invOrders.put( "P-A1",9);
        invOrders.put( "P",10);
        invOrders.put( "A1-A1-P",11);
        invOrders.put( "A1-P-A1",12);
        invOrders.put( "P-A1-A1",13);
        invOrders.put( "A0-A0-P",14);
        invOrders.put("A0-P-A0",15);
        invOrders.put( "P-A0-A0",16);

    }

    public HashMap<String, Integer> getInvOrders() {
        return invOrders;
    }

    public int getNumOrders() {
        return numOrders;
    }

    public HashMap<Integer, String> getOrders() {
        return orders;
    }

    public void setInvOrders(HashMap<String, Integer> invOrders) {
        this.invOrders = invOrders;
    }

    public void setNumOrders(int numOrders) {
        this.numOrders = numOrders;
    }

    public void setOrders(HashMap<Integer, String> orders) {
        this.orders = orders;
    }

    public String getOrderStr(int id){
        return orders.get(id);
    }
    public int getOrderId(String order){
//        System.out.println("order:"+order);
        return invOrders.get(order);
    }
}
