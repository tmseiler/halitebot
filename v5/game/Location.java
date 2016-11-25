package game;

public class Location {
    public int x, y;

    public Location(int x_, int y_) {
        x = x_;
        y = y_;
    }

    public Location(Location l) {
        x = l.x;
        y = l.y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Location location = (Location) o;

        return x == location.x && y == location.y;

    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ')';
    }
}
