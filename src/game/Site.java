package game;

public class Site {
    public int owner;
    public int strength;
    public int production;

    public double strengthProductionIndex;
    public double bolsterScore;

    public double clusterAcquisitionScore;

    public double individualAcquisitionScore() {
        strengthProductionIndex = (float) production / ((float) strength + 1);
        return strengthProductionIndex;
    }
}
