package game;

public class Piece {
    public int owner;
    public int strength;
    public int damageTaken;
    public boolean moved;

    public Piece(int owner, int strength) {
        this.owner = owner;
        this.strength = strength;
        this.damageTaken = 0;
        moved = false;
    }

    public Piece() {
        owner = 0;
        strength = 0;
        damageTaken = 0;
        moved = false;
    }

    public Piece(int owner, int strength, boolean moved) {
        this.owner = owner;
        this.strength = strength;
        this.moved = moved;
    }
}
