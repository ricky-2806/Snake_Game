import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/**
 * PS No. 76: a console Snake game on a 10 by 10 board.
 * Compile and run with: javac SnakeGame.java, then java SnakeGame.
 */
public class SnakeGame {
    private static final int SIZE = 10;
    private static final int EMPTY = 0;
    private static final int SNAKE = 1;
    private static final int FOOD = 2;

    // Each cell is encoded as row * 10 + column. The head is at the front.
    private final ArrayDeque<Integer> snake = new ArrayDeque<>();

    // HashSet membership checks whether a cell is occupied in constant time.
    private final HashSet<Integer> occupied = new HashSet<>();

    // This 2D array stores the board that is drawn in the console.
    private final char[][] board = new char[SIZE][SIZE];

    private final Random random = new Random();
    private int directionRow = 0;
    private int directionColumn = 1; // RIGHT
    private int food = encode(5, 7);
    private int score = 0;
    private boolean gameOver = false;

    /** Sets the required initial snake, direction, and food position. */
    public SnakeGame() {
        clearBoard();
        int startingHead = encode(5, 5);
        snake.addFirst(startingHead);
        occupied.add(startingHead);
        redrawBoard();
    }

    private void clearBoard() {
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                board[row][column] = '.';
            }
        }
    }

    /** Processes one valid direction key and advances one cell. */
    private boolean playTurn(char key) {
        char command = Character.toUpperCase(key);
        if (!isValidKey(command)) {
            System.out.println("Invalid key. Use W/A/S/D.");
            return !gameOver;
        }
        changeDirection(command);
        return moveOneCell();
    }

    private static boolean isValidKey(char key) {
        return key == 'W' || key == 'A' || key == 'S' || key == 'D';
    }

    private static boolean isValidInput(String input) {
        String trimmed = input.trim();
        return trimmed.length() == 1
                && isValidKey(Character.toUpperCase(trimmed.charAt(0)));
    }

    private void changeDirection(char key) {
        int nextRow = directionRow;
        int nextColumn = directionColumn;
        switch (key) {
            case 'W': nextRow = -1; nextColumn = 0; break;
            case 'A': nextRow = 0; nextColumn = -1; break;
            case 'S': nextRow = 1; nextColumn = 0; break;
            case 'D': nextRow = 0; nextColumn = 1; break;
            default: return;
        }

        // Ignore a direct reversal to prevent turning into the snake's neck.
        if (nextRow == -directionRow && nextColumn == -directionColumn) {
            return;
        }
        directionRow = nextRow;
        directionColumn = nextColumn;
    }

    private boolean moveOneCell() {
        if (gameOver) {
            return false;
        }

        int currentHead = snake.peekFirst();
        int nextRow = rowOf(currentHead) + directionRow;
        int nextColumn = columnOf(currentHead) + directionColumn;

        // A head outside the 10 by 10 board has hit a wall.
        if (nextRow < 0 || nextRow >= SIZE || nextColumn < 0 || nextColumn >= SIZE) {
            gameOver = true;
            return false;
        }

        int nextHead = encode(nextRow, nextColumn);
        boolean eating = nextHead == food;
        int tail = snake.peekLast();

        // The tail moves away on an ordinary turn, so its old cell is safe to enter.
        if (occupied.contains(nextHead) && (eating || nextHead != tail)) {
            gameOver = true;
            return false;
        }

        // Add the new head first so the deque always describes the full new body.
        snake.addFirst(nextHead);
        occupied.add(nextHead);

        if (eating) {
            score += 10;
            placeFood();
        } else {
            int oldTail = snake.removeLast();
            occupied.remove(oldTail);

            // If the head entered the tail's old cell, keep that cell occupied.
            if (oldTail == nextHead) {
                occupied.add(nextHead);
            }
        }

        redrawBoard();
        return !gameOver;
    }

    /** Rebuilds every cell from food and the current deque after a move. */
    private void redrawBoard() {
        clearBoard();
        if (food >= 0) {
            board[rowOf(food)][columnOf(food)] = '*';
        }

        for (int cell : snake) {
            board[rowOf(cell)][columnOf(cell)] = 'O';
        }

        int head = snake.peekFirst();
        board[rowOf(head)][columnOf(head)] = 'H';
    }

    /** Places food randomly on a cell not occupied by the snake. */
    private void placeFood() {
        if (occupied.size() == SIZE * SIZE) {
            food = -1;
            gameOver = true;
            return;
        }

        do {
            food = random.nextInt(SIZE * SIZE);
        } while (occupied.contains(food));
    }

    /** Shows a fresh, readable board and the current score and length. */
    private void drawBoard() {
        System.out.println("========== SNAKE GAME | PS No. 76 ==========");
        System.out.println("Score: " + score + "    Length: " + snake.size());
        System.out.println("+---------------------+");
        for (int row = 0; row < SIZE; row++) {
            System.out.print("| ");
            for (int column = 0; column < SIZE; column++) {
                System.out.print(board[row][column] + " ");
            }
            System.out.println("|");
        }
        System.out.println("+---------------------+");
    }

    /**
     * Reads scores, adds the current score, sorts them with bubble sort,
     * keeps at most five, and writes them back to the supplied file.
     */
    private static List<Integer> updateLeaderboard(Path scoreFile, int currentScore)
            throws IOException {
        List<Integer> scores = new ArrayList<>();

        if (Files.exists(scoreFile)) {
            for (String line : Files.readAllLines(scoreFile)) {
                try {
                    scores.add(Integer.parseInt(line.trim()));
                } catch (NumberFormatException ignored) {
                    // Skip blank or malformed lines and continue loading scores.
                }
            }
        }
        scores.add(currentScore);

        // Custom bubble sort: highest score first. No built-in sorting is used.
        for (int pass = 0; pass < scores.size() - 1; pass++) {
            for (int index = 0; index < scores.size() - 1 - pass; index++) {
                if (scores.get(index) < scores.get(index + 1)) {
                    int temporary = scores.get(index);
                    scores.set(index, scores.get(index + 1));
                    scores.set(index + 1, temporary);
                }
            }
        }

        List<Integer> topScores = new ArrayList<>();
        List<String> linesToSave = new ArrayList<>();
        for (int index = 0; index < scores.size() && index < 5; index++) {
            topScores.add(scores.get(index));
            linesToSave.add(Integer.toString(scores.get(index)));
        }
        Files.write(scoreFile, linesToSave);
        return topScores;
    }

    private void finishGame() {
        System.out.println();
        System.out.println("Game Over!");
        System.out.println("Final Score: " + score);
        System.out.println("Final Length: " + snake.size());

        try {
            List<Integer> topScores = updateLeaderboard(Paths.get("scores.txt"), score);
            System.out.println("Top 5 Leaderboard");
            if (topScores.isEmpty()) {
                System.out.println("No scores saved yet.");
            } else {
                for (int index = 0; index < topScores.size(); index++) {
                    System.out.println((index + 1) + ". " + topScores.get(index));
                }
            }
        } catch (IOException error) {
            System.out.println("Could not update scores.txt: " + error.getMessage());
        }
    }

    private static int encode(int row, int column) {
        return row * SIZE + column;
    }

    private static int rowOf(int cell) {
        return cell / SIZE;
    }

    private static int columnOf(int cell) {
        return cell % SIZE;
    }

    private void run() {
        Scanner input = new Scanner(System.in);
        System.out.println("Move with W/A/S/D. Press Enter after each key.");

        while (!gameOver) {
            drawBoard();
            String line;
            while (true) {
                System.out.print("Move (W/A/S/D): ");
                if (!input.hasNextLine()) {
                    return;
                }

                line = input.nextLine().trim();
                if (line.isEmpty()) {
                    // Enter alone does not advance the snake.
                    continue;
                }
                if (!isValidInput(line)) {
                    System.out.println("Invalid key. Use W/A/S/D.");
                    continue;
                }
                break;
            }

            boolean alive = playTurn(line.charAt(0));
            if (alive) {
                System.out.println("\n\n");
            }
        }
        finishGame();
    }

    /* Deterministic fixture for the assignment's boundary and longer-snake cases. */
    private static SnakeGame testGame(int[] headFirstBody, char direction, int startingScore) {
        SnakeGame game = new SnakeGame();
        game.clearBoard();
        game.snake.clear();
        game.occupied.clear();
        for (int cell : headFirstBody) {
            game.snake.addLast(cell);
            game.occupied.add(cell);
            game.board[rowOf(cell)][columnOf(cell)] = 'O';
        }
        int head = game.snake.peekFirst();
        game.board[rowOf(head)][columnOf(head)] = 'H';
        game.directionRow = 0;
        game.directionColumn = 1;
        game.changeDirection(direction);
        game.score = startingScore;
        game.food = encode(9, 9);
        game.board[9][9] = '*';
        game.gameOver = false;
        return game;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("FAIL: " + message);
        }
        System.out.println("PASS: " + message);
    }

    /** Run assignment checks with: java SnakeGame --test */
    private static void runTests() {
        SnakeGame normal = new SnakeGame();
        normal.playTurn('D');
        normal.playTurn('D');
        require(normal.score == 10 && normal.snake.size() == 2
                        && normal.snake.peekFirst() == encode(5, 7),
                "D,D eats food: score 10 and length 2");

        SnakeGame wall = testGame(new int[] {encode(0, 0)}, 'W', 0);
        boolean wallAlive = wall.playTurn('W');
        require(!wallAlive && wall.gameOver && wall.score == 0,
                "head at (0,0) moving UP hits the wall");

        SnakeGame invalid = new SnakeGame();
        invalid.playTurn('X');
        require(invalid.snake.peekFirst() == encode(5, 5)
                        && invalid.directionRow == 0 && invalid.directionColumn == 1
                        && !isValidInput("") && !isValidInput("aaa")
                        && !isValidInput("sdsaxs") && isValidInput("w"),
                "empty, multi-character, and invalid input do not move; lowercase W is valid");

        // Test fixture starts with score 40; the S,A,W moves cause no food pickup.
        SnakeGame longer = testGame(new int[] {57, 56, 55, 54, 53}, 'D', 40);
        longer.playTurn('S');
        longer.playTurn('A');
        boolean longerAlive = longer.playTurn('W');
        require(!longerAlive && longer.gameOver && longer.score == 40
                        && longer.snake.size() == 5,
                "long snake and S,A,W cause self-collision with score 40 and length 5");

        Path testFile = null;
        try {
            testFile = Files.createTempFile("snake-scores", ".txt");
            List<String> oldScores = new ArrayList<>();
            oldScores.add("30");
            oldScores.add("10");
            Files.write(testFile, oldScores);
            List<Integer> top = updateLeaderboard(testFile, longer.score);
            require(top.size() == 3 && top.get(0) == 40 && top.get(1) == 30
                            && top.get(2) == 10,
                    "scores are saved and sorted as 40, 30, 10");
        } catch (IOException error) {
            throw new IllegalStateException("Score persistence test failed", error);
        } finally {
            if (testFile != null) {
                try {
                    Files.deleteIfExists(testFile);
                } catch (IOException ignored) {
                    // The temporary test file is outside the game data file.
                }
            }
        }

        System.out.println("All Snake Game tests passed.");
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--test")) {
            runTests();
        } else {
            new SnakeGame().run();
        }
    }
}
