package game;

public class Site {
    public int owner;
    public int strength;
    public int production;
    public boolean isFriendly;
    public Location location;

    public double clusterAcquisitionScore;

    public double individualAcquisitionScore() {
        return (double) production / (double) Math.max(strength, 1);
//        strengthProductionIndex = (double) production * (double) production * (double) production;
//        strengthProductionIndex = (double);
    }
}
