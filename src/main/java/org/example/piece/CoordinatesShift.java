package org.example.piece;

public class CoordinatesShift {
    public final int fileShift;
    public final int rankShift;

    public CoordinatesShift(int fileShift, int rankShift) {
        this.fileShift = fileShift;
        this.rankShift = rankShift;
    }

    @Override
    public String toString() {
        return "CoordinatesShift{" +
                "fileShift=" + fileShift +
                ", rankShift=" + rankShift +
                '}';
    }
}
