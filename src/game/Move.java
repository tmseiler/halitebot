package game;

public class Move {
    public Location loc;
    public Direction dir;
    public int owner;

    public Move(Location loc_, Direction dir_, int owner) {
        loc = loc_;
        dir = dir_;
        this.owner = owner;
    }

    @Override
    public String toString() {
        return "Move{" +
                "loc=" + loc +
                ", dir=" + dir +
                '}';
    }
}
