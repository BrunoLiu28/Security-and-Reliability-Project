/**
 * Class for wine.
 *
 * @author Bruno Liu fc56297
 * @author Duarte Pinheiro fc54475
 * @author Rodrigo Cancelinha fc56371
 */
public class Wine {
    private String name;
    private String image;
    private int[] classification;

    /**
     * The constructor for Wine class.
     *
     * @param wineName The wine's name.
     * @param image    The wine's image.
     */
    public Wine(String wineName, String image) {
        this.classification = new int[5];
        this.name = wineName;
        this.image = image;
    }

    /**
     * Getter for the wine's name.
     *
     * @return The wine's name.
     */
    public String getWineName() {
        return name;
    }

    /**
     * Getter for the wine's image
     *
     * @return The wine's image
     */
    public String getImage() {
        return image;
    }

    /**
     * Getter for the list of classifications.
     *
     * @return The list of classifications.
     */
    public int[] getClassifications() {
        return this.classification;
    }

    /**
     * Calculates the average classification.
     *
     * @return The average classification.
     */
    public Double getAverageClassification() {
        Double total = 0.0;
        int howMany = 0;
        for (int i = 0; i < classification.length; i++) {
            total += (i + 1) * classification[i];
            howMany += classification[i];
        }
        return (double) (Math.round((total / howMany) * 100)) / 100;
    }

    /**
     * Setter for the wine classification.
     *
     * @param classification The classifications given.
     */
    public void setClassifications(int[] classification) {
        this.classification = classification;
    }

    /**
     * Adds a star in the classification.
     *
     * @param starNumber the starnumber that was given.
     *
     * @requires {@code 1 <= starNumber <=5}
     */
    public void addStar(int starNumber) {
        this.classification[starNumber] += 1;
    }

}
