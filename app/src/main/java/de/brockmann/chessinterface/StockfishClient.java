package de.brockmann.chessinterface;

import com.github.bagaturchess.stockfish.engine.Stockfish;

/**
 * Wrapper around the Bagatur stockfish-android library.
 */
public class StockfishClient {
    private Stockfish engine;

    public boolean start() {
        engine = new Stockfish();
        return engine.startEngine();
    }

    public void stop() {
        if (engine != null) {
            engine.stopEngine();
        }
    }

    public void setElo(int elo) {
        engine.sendCommand("setoption name UCI_LimitStrength value true");
        engine.sendCommand("setoption name UCI_Elo value " + elo);
        engine.sendCommand("isready");
    }

    public String getBestMove(String fen, int movetimeMs) {
        engine.sendCommand("position fen " + fen);
        return engine.goForBestMove(movetimeMs);
    }
}
