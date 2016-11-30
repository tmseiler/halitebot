package game;

public class Site {
    public int owner;
    public int strength;
    public int production;
    public boolean isFriendly;

    public double strengthProductionIndex;

    public double clusterAcquisitionScore;

    public double individualAcquisitionScore() {
        strengthProductionIndex = (double) production * (double) production / Math.pow(strength + 1.0, 1.8);
        return strengthProductionIndex;
    }
}
