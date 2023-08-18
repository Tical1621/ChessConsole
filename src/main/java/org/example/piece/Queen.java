package org.example.piece;

import org.example.movement.Coordinates;
import org.example.movement.IBishop;

import java.util.Set;

public class Queen extends LongRangePiece implements IBishop,IRook {
    public Queen(Color color, Coordinates coordinates) {
        super(color, coordinates);
    }

    @Override
    protected Set<CoordinatesShift> getPieceMoves() {
        //хороший ход с выносом методов двух фигур в интерфейс
        Set<CoordinatesShift> moves = getBishopMoves();
        moves.addAll(getRookMoves());
        return moves;
    }
}
