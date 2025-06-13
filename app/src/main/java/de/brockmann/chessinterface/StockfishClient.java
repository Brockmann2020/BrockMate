package de.brockmann.chessinterface;

import java.io.*;

/**
 * Minimal wrapper to communicate with a Stockfish engine binary
 * via the UCI protocol.
 */
public class StockfishClient {
    private Process engine;
    private BufferedReader reader;
    private BufferedWriter writer;

    public boolean start() {
        try {
            engine = new ProcessBuilder("stockfish")
                    .redirectErrorStream(true)
                    .start();
            reader = new BufferedReader(new InputStreamReader(engine.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(engine.getOutputStream()));
            sendCommand("uci");
            waitForReady();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void stop() {
        if (engine != null) {
            sendCommand("quit");
            engine.destroy();
        }
    }

    private void sendCommand(String cmd) {
        try {
            writer.write(cmd + "\n");
            writer.flush();
        } catch (IOException ignore) {
        }
    }

    private void waitForReady() {
        sendCommand("isready");
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("readyok")) break;
            }
        } catch (IOException ignore) {
        }
    }

    public void setElo(int elo) {
        sendCommand("setoption name UCI_LimitStrength value true");
        sendCommand("setoption name UCI_Elo value " + elo);
        waitForReady();
    }

    public String getBestMove(String fen, int movetimeMs) {
        sendCommand("position fen " + fen);
        sendCommand("go movetime " + movetimeMs);
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("bestmove")) {
                    String[] parts = line.split(" ");
                    return parts.length > 1 ? parts[1] : null;
                }
            }
        } catch (IOException ignore) {
        }
        return null;
    }
}
