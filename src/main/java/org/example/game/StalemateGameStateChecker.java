package org.example.game;

import org.example.piece.Color;
import org.example.board.Board;
import org.example.movement.Coordinates;
import org.example.piece.Piece;

import java.util.List;
import java.util.Set;

public class StalemateGameStateChecker extends GameStateChecker {
    @Override
    public GameState check(Board board, Color color) {
        List<Piece> pieces = board.getPiecesByColor(color);
        for(Piece piece : pieces) {
            Set<Coordinates> availableMoveSquares = piece.getAvailableMoveSquares(board);
            if(!availableMoveSquares.isEmpty()) {
                return GameState.ONGOING;
            }
        }
        return GameState.STALEMATE;
    }
}
