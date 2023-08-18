package org.example.game;

import org.example.board.Board;
import org.example.board.BoardConsoleRenderer;
import org.example.movement.InputCoordinates;
import org.example.movement.Move;
import org.example.piece.Color;

import java.util.List;

public class Game {

    private final Board board;//доску передавать;

    private final List<GameStateChecker> checkers = List.of(new StalemateGameStateChecker(),
            new CheckmateGameStateChecker());
    private BoardConsoleRenderer renderer = new BoardConsoleRenderer();//рендерер создавать

    public Game(Board board) {
        this.board = board;
    }

    public void gameLoop() {
        Color colorToMove = Color.BLACK;
        GameState state = determineGameState(board, colorToMove);
       while(state == GameState.ONGOING) {
           renderer.render(board);
            if(colorToMove == Color.WHITE) {
                System.out.println("white to move");
            } else {
                System.out.println("Black to move");
            }
            Move move = InputCoordinates.inputMove(board, colorToMove, renderer);
            //make move
            board.makeMove(move);
            //pass move
           colorToMove = colorToMove.opposite();
            state = determineGameState(board, colorToMove);
       }

       renderer.render(board);
       System.out.println("game ended with state: " + state);
    }

    private GameState determineGameState(Board board, Color color) {
        for(GameStateChecker checker : checkers) {
            GameState state = checker.check(board, color);
            if(state !=GameState.ONGOING) {
                return state;
            }
        }
        return GameState.ONGOING;
    }
}
