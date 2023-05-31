/**
 * Class for client.
 * 
 * @author Bruno Liu fc56297
 * @author Duarte Pinheiro fc54475
 * @author Rodrigo Cancelinha fc56371
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Client {
    private String clientId;
    private String password;

    private byte[] publicKey;
    private Double balance;
    private Map<String,ArrayList<String>> boxMessage;

    /**
     * The constructor for Client class.
     *
     * @param id   Client's ID.
     * @param publicKey Client's public key.
     */
    public Client(String id, byte[] publicKey){
        this.clientId = id;
//        this.password = pass;
        this.publicKey = publicKey;
        this.balance = 200.00;
        this.boxMessage = new HashMap<String,ArrayList<String>>();
    }

    /**
     * Getter for client's ID.
     * 
     * @return The client's ID.
     */
    public String getClientId(){
        return this.clientId;
    }

    /**
     * Getter for client's balance.
     * 
     * @return The client's balance.
     */
    public Double getBalance(){
        return this.balance;
    }

    /**
     * Sets the client balance.
     * 
     * @param balance The new client balance.
     */
    public void setBalance(Double balance){
        this.balance =balance;
    }

    /**
     * Function that gives the money from a payment.
     * 
     * @param amount
     */
    public void pay(Double amount) {
        this.balance -= amount;
    }

    /**
     * Function that receives the money from a payment.
     */
    public void receive(Double amount) {
        this.balance += amount;
    }

    /**
     * Getter for client's password.
     * 
     * @return the client's password.
     */
    public String getPassword(){
        return this.password;
    }


    /**
     * Getter for client's BoxMessage.
     * 
     * @return the client's BoxMessage.
     */
    public Map<String, ArrayList<String>> getBoxMessage(){
        return this.boxMessage;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    /**
     * Add a massage in the client's box message.
     * 
     * @param userSender    The sender.
     * @param message       The sender's message.
     */
    public void addMessages(String userSender, String message){
        if(boxMessage.containsKey(userSender)){
            ArrayList<String> aux = this.boxMessage.get(userSender);
            aux.add(message);
        }else{
            ArrayList<String> aux = new ArrayList<String>();
            aux.add(message);
            this.boxMessage.put(userSender, aux);
        }
    }

    /**
     * Read all client's message and clears the box message.
     * 
     * @return All client's message.
     */
    public String readMessages(){
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ArrayList<String>> set : this.boxMessage.entrySet()) {
            ArrayList<String> messages = set.getValue();
            for (int i = 0; i < messages.size(); i++) {
                sb.append(set.getKey() + " send you: " + messages.get(i) + "\n");
            }
        }
        this.boxMessage.clear();
        return sb.toString();
    }



}
