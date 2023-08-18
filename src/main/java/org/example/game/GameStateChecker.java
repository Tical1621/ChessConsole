package org.example.game;

import org.example.piece.Color;
import org.example.board.Board;

public abstract  class GameStateChecker {
    public abstract GameState check(Board board, Color color);
}
