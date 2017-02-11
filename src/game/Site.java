package game;

public class Site {
    public int owner;
    public int strength;
    public int production;
    public boolean isFriendly;
    public Location location;

    public double score() {
        return Math.pow(production, 2.2) / (Math.pow(strength + 1.0, 2.5));
//        return (double) production * (double) production / (double) strength + 1.0;
//        return production * Math.exp(-.01 * strength);
    }
}
