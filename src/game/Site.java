package game;

public class Site {
    public int owner;
    public int strength;
    public int production;
    public boolean isFriendly;
    public Location location;

    public double clusterAcquisitionScore;

    public double individualAcquisitionScore() {
        return Math.pow(production, 2.2) / Math.pow(strength + 1.0, 1.8);
    }
}
