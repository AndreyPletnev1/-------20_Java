import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;


public class Main {
    private static final String ALPHABET = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ .";
    private static final Path DATA_DIR = Path.of("data");

    public static void main(String[] args) {
        try (Scanner console = new Scanner(System.in, StandardCharsets.UTF_8)) {
            System.out.println("Вариант 20 - симметричные методы шифрования");
            System.out.println("1 - таблица Трисемуса (метод 5)");
            System.out.println("2 - двойная табличная перестановка (метод 10)");
            System.out.println("0 - выход");
            System.out.print("Выберите метод: ");
            String choice = console.nextLine().trim();

            if (choice.equals("0")) {
                System.out.println("Работа программы завершена.");
                return;
            }
            if (!choice.equals("1") && !choice.equals("2")) {
                System.out.println("Ошибка: выберите способ 1 или 2.");
                return;
            }

            if (choice.equals("1")) {
                boolean decrypt = chooseOperation(console);
                runTrisemus(decrypt);
            } else {
                boolean decrypt = chooseOperation(console);
                runDoubleTransposition(decrypt);
            }
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println("Ошибка: " + exception.getMessage());
        }
    }

    private static boolean chooseOperation(Scanner console) {
        System.out.println("1 - шифрование");
        System.out.println("2 - расшифровка");
        System.out.print("Выберите действие: ");
        String choice = console.nextLine().trim();
        if (choice.equals("1")) return false;
        if (choice.equals("2")) return true;
        throw new IllegalArgumentException("выберите действие 1 или 2.");
    }

    private static void runTrisemus(boolean decrypt) throws IOException {
        String key = readFile(DATA_DIR.resolve("key_trisemus.txt")).trim();
        Path resultPath = DATA_DIR.resolve("result_trisemus.txt");
        String input = readFile(decrypt ? resultPath : DATA_DIR.resolve("input.txt"));
        validateAlphabet(input, decrypt ? "Шифртекст" : "Входной текст");

        String table = createTrisemusTable(key);
        char[] mapping = createTrisemusMapping(table);
        String encryptedAlphabet = trisemus(ALPHABET, mapping);
        char[] activeMapping = decrypt ? createTrisemusInverseMapping(mapping) : mapping;
        String output = trisemus(input, activeMapping);
        String inputLabel = decrypt ? "Шифртекст: " : "Исходный текст: ";
        String outputLabel = decrypt ? "Расшифрованный текст: " : "Зашифрованный текст: ";
        String report = makeReport(
                "Метод: таблица Трисемуса (5 x 7) — " + (decrypt ? "расшифровка" : "шифрование"),
                "Ключевое слово: " + key,
                "Основной алфавит:",
                renderGrid(ALPHABET, 5, 7),
                "Таблица Трисемуса:",
                renderGrid(table, 5, 7),
                "Зашифрованная версия алфавита:",
                renderGrid(encryptedAlphabet, 5, 7),
                inputLabel + input,
                outputLabel + output);
        System.out.print(report);
        if (!decrypt) {
            Files.writeString(resultPath, output, StandardCharsets.UTF_8);
            System.out.println("\nШифртекст сохранён в файле: " + resultPath);
        } else {
            System.out.println("\nШифртекст оставлен в файле: " + resultPath);
        }
    }

    private static void runDoubleTransposition(boolean decrypt) throws IOException {
        List<String> keys = Files.readAllLines(DATA_DIR.resolve("keys_double.txt"), StandardCharsets.UTF_8);
        if (keys.size() < 2) {
            throw new IllegalArgumentException("в data/keys_double.txt нужны два ключа: столбцы и строки.");
        }
        String columnKey = keys.get(0).trim().toUpperCase(Locale.ROOT);
        String rowKey = keys.get(1).trim();
        DoubleTransposition cipher = new DoubleTransposition(columnKey, rowKey);

        Path resultPath = DATA_DIR.resolve("result_double_transposition.txt");
        String input = readFile(decrypt ? resultPath : DATA_DIR.resolve("input.txt"));
        validateAlphabet(input, decrypt ? "Шифртекст" : "Входной текст");
        String encryptedAlphabet = cipher.encrypt(ALPHABET);
        String output = decrypt
                ? cipher.decryptUsingAlphabetOrder(input)
                : cipher.encryptUsingAlphabetOrder(input);
        String inputLabel = decrypt ? "Шифртекст: " : "Исходный текст: ";
        String outputLabel = decrypt ? "Расшифрованный текст: " : "Зашифрованный текст: ";
        String report = makeReport(
                "Метод: двойная табличная перестановка (10) — " + (decrypt ? "расшифровка" : "шифрование"),
                "Ключ столбцов: " + columnKey,
                "Порядок строк: " + rowKey,
                "Размер таблицы: " + rowKey.length() + " x " + columnKey.length(),
                "Основной алфавит:",
                renderBlocks(ALPHABET, rowKey.length(), columnKey.length()),
                "Зашифрованная версия алфавита (показывает порядок перестановки):",
                renderBlocks(encryptedAlphabet, rowKey.length(), columnKey.length()),
                "Пояснение: порядок символов из первой полной таблицы зашифрованного алфавита применяется к каждому блоку текста.",
                "В неполном последнем блоке используются только позиции, в которых есть символы.",
                inputLabel + input,
                outputLabel + output);
        System.out.print(report);
        if (!decrypt) {
            Files.writeString(resultPath, output, StandardCharsets.UTF_8);
            System.out.println("\nШифртекст сохранён в файле: " + resultPath);
        } else {
            System.out.println("\nШифртекст оставлен в файле: " + resultPath);
        }
    }

    private static String createTrisemusTable(String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("ключ Трисемуса пуст.");
        }
        validateAlphabet(key, "Ключ Трисемуса");
        Set<Character> symbols = new LinkedHashSet<>();
        for (int i = 0; i < key.length(); i++) symbols.add(key.charAt(i));
        for (int i = 0; i < ALPHABET.length(); i++) symbols.add(ALPHABET.charAt(i));

        StringBuilder table = new StringBuilder(symbols.size());
        for (char symbol : symbols) table.append(symbol);
        if (table.length() != 35) {
            throw new IllegalStateException("алфавит варианта 20 должен содержать ровно 35 символов.");
        }
        return table.toString();
    }

    private static char[] createTrisemusMapping(String table) {
        int rows = 5;
        int columns = 7;
        char[] mapping = new char[ALPHABET.length()];
        for (int i = 0; i < ALPHABET.length(); i++) {
            int tableIndex = table.indexOf(ALPHABET.charAt(i));
            int row = tableIndex / columns;
            int column = tableIndex % columns;
            mapping[i] = table.charAt(((row + 1) % rows) * columns + column);
        }
        return mapping;
    }

    private static char[] createTrisemusInverseMapping(char[] mapping) {
        char[] inverse = new char[ALPHABET.length()];
        for (int i = 0; i < mapping.length; i++) {
            int encryptedIndex = ALPHABET.indexOf(mapping[i]);
            if (encryptedIndex < 0 || inverse[encryptedIndex] != '\0') {
                throw new IllegalStateException("таблица Трисемуса не задаёт обратимую замену.");
            }
            inverse[encryptedIndex] = ALPHABET.charAt(i);
        }
        return inverse;
    }

    private static String trisemus(String text, char[] mapping) {
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char symbol = text.charAt(i);
            int index = ALPHABET.indexOf(symbol);
            if (index < 0) {
                throw new IllegalArgumentException("символ отсутствует в алфавите варианта 20.");
            }
            result.append(mapping[index]);
        }
        return result.toString();
    }

    private static String readFile(Path path) throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        return text.replaceFirst("[\\r\\n]+$", "").toUpperCase(Locale.ROOT);
    }

    private static void validateAlphabet(String text, String label) {
        for (int i = 0; i < text.length(); i++) {
            char symbol = text.charAt(i);
            if (ALPHABET.indexOf(symbol) < 0) {
                String shown;
                if (symbol == ' ') {
                    shown = "пробел";
                } else {
                    shown = "'" + symbol + "'";
                }
                throw new IllegalArgumentException(label + ": символ " + shown
                        + " отсутствует в алфавите варианта 20.");
            }
        }
    }

    private static String renderBlocks(String text, int rows, int columns) {
        StringBuilder result = new StringBuilder();
        int blockSize = rows * columns;
        int blockNumber = 1;
        for (int offset = 0; offset < text.length(); offset += blockSize) {
            int end = Math.min(offset + blockSize, text.length());
            result.append("Блок ").append(blockNumber).append(System.lineSeparator());
            result.append(renderGrid(text.substring(offset, end), rows, columns));
            blockNumber++;
        }
        return result.toString();
    }

    private static String renderGrid(String text, int rows, int columns) {
        StringBuilder result = new StringBuilder();
        int index = 0;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                if (index < text.length()) {
                    char symbol = text.charAt(index);
                    if (symbol == ' ') {
                        result.append('_');
                    } else {
                        result.append(symbol);
                    }
                    index++;
                }
                if (column < columns - 1 && index < text.length()) result.append(' ');
            }
            result.append(System.lineSeparator());
        }
        return result.toString();
    }

    private static String makeReport(String... lines) {
        return String.join(System.lineSeparator(), lines) + System.lineSeparator();
    }

    /** Двойная перестановка строк и столбцов, заданная двумя ключами. */
    private static final class DoubleTransposition {
        private final int rows;
        private final int columns;
        private final int[] columnOrder;
        private final int[] rowOrder;

        private DoubleTransposition(String columnKey, String rowKey) {
            if (columnKey.isEmpty() || rowKey.isEmpty()) {
                throw new IllegalArgumentException("ключи перестановки не должны быть пустыми.");
            }
            rows = rowKey.length();
            columns = columnKey.length();
            columnOrder = alphabeticalOrder(columnKey);
            rowOrder = numericOrder(rowKey);
        }

        private String encrypt(String text) {
            StringBuilder result = new StringBuilder(text.length());
            int capacity = rows * columns;
            for (int offset = 0; offset < text.length(); offset += capacity) {
                int blockLength = Math.min(capacity, text.length() - offset);
                char[][] original = new char[rows][columns];
                boolean[][] present = new boolean[rows][columns];
                for (int i = 0; i < blockLength; i++) {
                    original[i / columns][i % columns] = text.charAt(offset + i);
                    present[i / columns][i % columns] = true;
                }

                char[][] columnsMoved = new char[rows][columns];
                boolean[][] columnsPresent = new boolean[rows][columns];
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < columns; c++) {
                        columnsMoved[r][c] = original[r][columnOrder[c]];
                        columnsPresent[r][c] = present[r][columnOrder[c]];
                    }
                }

                char[][] finalTable = new char[rows][columns];
                boolean[][] finalPresent = new boolean[rows][columns];
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < columns; c++) {
                        finalTable[r][c] = columnsMoved[rowOrder[r]][c];
                        finalPresent[r][c] = columnsPresent[rowOrder[r]][c];
                    }
                }

                // По методичке шифртекст считывается по столбцам.
                for (int c = 0; c < columns; c++) {
                    for (int r = 0; r < rows; r++) {
                        if (finalPresent[r][c]) {
                            result.append(finalTable[r][c]);
                        }
                    }
                }
            }
            return result.toString();
        }

        /**
         * Шифрует текст по порядку индексов, который задаёт зашифрованная
         * версия первых blockSize символов алфавита. Это та же перестановка,
         * что и в encrypt(), в том числе для неполного последнего блока.
         */
        private String encryptUsingAlphabetOrder(String text) {
            int blockSize = rows * columns;
            if (blockSize > ALPHABET.length()) {
                throw new IllegalArgumentException("размер таблицы не должен превышать длину алфавита ("
                        + ALPHABET.length() + " символов).");
            }

            String alphabetBlock = ALPHABET.substring(0, blockSize);
            String encryptedAlphabetBlock = encrypt(alphabetBlock);
            int[] permutation = new int[blockSize];
            for (int i = 0; i < blockSize; i++) {
                int sourceIndex = alphabetBlock.indexOf(encryptedAlphabetBlock.charAt(i));
                if (sourceIndex < 0) {
                    throw new IllegalStateException("не удалось определить порядок по зашифрованному алфавиту.");
                }
                permutation[i] = sourceIndex;
            }

            StringBuilder result = new StringBuilder(text.length());
            for (int offset = 0; offset < text.length(); offset += blockSize) {
                int blockLength = Math.min(blockSize, text.length() - offset);
                for (int index : permutation) {
                    if (index < blockLength) {
                        result.append(text.charAt(offset + index));
                    }
                }
            }
            return result.toString();
        }

        private String decryptUsingAlphabetOrder(String text) {
            int blockSize = rows * columns;
            if (blockSize > ALPHABET.length()) {
                throw new IllegalArgumentException("размер таблицы не должен превышать длину алфавита ("
                        + ALPHABET.length() + " символов).");
            }

            String alphabetBlock = ALPHABET.substring(0, blockSize);
            String encryptedAlphabetBlock = encrypt(alphabetBlock);
            int[] permutation = new int[blockSize];
            for (int i = 0; i < blockSize; i++) {
                int sourceIndex = alphabetBlock.indexOf(encryptedAlphabetBlock.charAt(i));
                if (sourceIndex < 0) {
                    throw new IllegalStateException("не удалось определить порядок по зашифрованному алфавиту.");
                }
                permutation[i] = sourceIndex;
            }

            StringBuilder result = new StringBuilder(text.length());
            for (int offset = 0; offset < text.length(); offset += blockSize) {
                int blockLength = Math.min(blockSize, text.length() - offset);
                char[] restored = new char[blockLength];
                int cipherIndex = 0;
                for (int sourceIndex : permutation) {
                    if (sourceIndex < blockLength) {
                        restored[sourceIndex] = text.charAt(offset + cipherIndex++);
                    }
                }
                result.append(restored);
            }
            return result.toString();
        }

        private static int[] alphabeticalOrder(String key) {
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < key.length(); i++) indices.add(i);
            indices.sort(Comparator.comparingInt((Integer i) -> key.charAt(i)).thenComparingInt(i -> i));
            return indices.stream().mapToInt(Integer::intValue).toArray();
        }

        /** Цифра в каждой позиции задаёт приоритет соответствующей строки. */
        private static int[] numericOrder(String key) {
            int count = key.length();
            boolean[] used = new boolean[count + 1];
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                char digit = key.charAt(i);
                if (digit < '1' || digit > '9') {
                    throw new IllegalArgumentException("ключ строк должен состоять из цифр 1…9.");
                }
                int rank = digit - '0';
                if (rank > count || used[rank]) {
                    throw new IllegalArgumentException("ключ строк должен быть перестановкой чисел от 1 до его длины.");
                }
                used[rank] = true;
                indices.add(i);
            }
            indices.sort(Comparator.comparingInt(i -> key.charAt(i) - '0'));
            return indices.stream().mapToInt(Integer::intValue).toArray();
        }
    }
}
