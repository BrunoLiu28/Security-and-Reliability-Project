/**
 * Class used for the relation between clients and wines.
 * 
 * @author Bruno Liu fc56297
 * @author Duarte Pinheiro fc54475
 * @author Rodrigo Cancelinha fc56371
 */
public class ClientAndWines {
    private String userId;
    private Double price;
    private int amount;


    /**
     * The constructor for ClientAndWines class.
     * 
     * @param userId      The client.
     * @param price     The price of the wine.
     * @param amount    The amount the client is selling.
     */
    public ClientAndWines(String userId, Double price, int amount){
        this.userId = userId;
        this.price = price;
        this.amount = amount;
    }

    /**
     * Getter for who is selling a certain wine.
     * 
     * @return The client that is selling a certain wine.
     */
    public String getUserID(){
        return userId;
    }

    /**
     * Getter for the wine's price from a certain seller.
     * 
     * @return The price of a wine from a certain seller.
     */
    public Double getPrice(){
        return price;
    }

    /**
     * Getter for a wine's amount available from a certain seller.
     * 
     * @return The amount available of a wine from a certain seller.
     */
    public int getAmount(){
        return amount;
    }

    /**
     * Adds the amount available of a wine from a certain seller.
     * 
     * @param add The amount added.
     */
    public void addAmount(int add){
        this.amount += add;
    }

    /**
     * Takes the amount available of a wine from a certain seller.
     * 
     * @param take The amount taken.
     * @requires {@code take <= this.amount}
     */
    public void takeAmount(int take){
        this.amount -= take;
    }

    /**
     * Uptades the price of the current wine.
     * 
     * @param newPrice The new price.
     */
    public void updatePrice(Double newPrice) {
        this.price = newPrice;
    }
}
