package org.example.piece;

public enum Color {

    WHITE,
    BLACK;

    public Color opposite() {
        //найс способ вернуть противоположный текущему цвет
        return this == WHITE ? BLACK : WHITE;
    }
}
