package game;

public class Move {
    public Location loc;
    public Direction dir;

    public Move(Location loc_, Direction dir_) {
        loc = loc_;
        dir = dir_;
    }

    @Override
    public String toString() {
        return "Move{" +
                "loc=" + loc +
                ", dir=" + dir +
                '}';
    }
}
