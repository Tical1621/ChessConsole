package org.example.game;

import org.example.piece.Color;
import org.example.board.Board;
import org.example.board.BoardFactory;
import org.example.movement.Coordinates;
import org.example.movement.Move;
import org.example.piece.King;
import org.example.piece.Piece;

import java.util.List;
import java.util.Set;

public class CheckmateGameStateChecker extends GameStateChecker {
    @Override
    public GameState check(Board board, Color color) {
        Piece king = board.getPiecesByColor(color).stream().filter(piece -> piece instanceof King).findFirst().get();
        if(!board.isSquareAttackedByColor(king.coordinates, color.opposite())) {
            return GameState.ONGOING;
        }
        List<Piece> pieces = board.getPiecesByColor(color);
        for(Piece piece: pieces) {
            Set<Coordinates> availableMoveSquares = piece.getAvailableMoveSquares(board);
            for(Coordinates coordinates: availableMoveSquares) {
                Board clone = new BoardFactory().copy(board);
                clone.makeMove(new Move(piece.coordinates, coordinates));
                Piece clonedKing = clone.getPiecesByColor(color).stream().filter(p -> p instanceof King).findFirst().get();
                if(!clone.isSquareAttackedByColor(clonedKing.coordinates, color.opposite())) {
                    return GameState.ONGOING;
                }
            }
        }
        if(color == Color.WHITE) {
            return GameState.CHECKMATE_TO_WHITE_KING;
        } else {
            return GameState.CHECKMATE_TO_BLACK_KING;
        }
    }
}
