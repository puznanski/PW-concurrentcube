package concurrentcube;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;


public class CubeTest {
    private final String TESTS_PATH = "src/concurrentcube/tests/";

    @Test
    void rotationsTest10() {
        Cube cube = new Cube(10, (x, y) -> {}, (x, y) -> {}, () -> {}, () -> {});
        StringBuilder actual = new StringBuilder();

        try {
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < 10; j++) {
                    cube.rotate(i, j);
                    actual.append(cube.show());
                    actual.append("\n");
                }
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        StringBuilder expected = new StringBuilder();

        try {
            File file = new File(TESTS_PATH + "rotationsTest10Expected.txt");
            Scanner reader = new Scanner(file);

            while (reader.hasNextLine()) {
                expected.append(reader.nextLine()).append("\n");
            }

            reader.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        assertEquals(expected.toString(), actual.toString());
    }

    @Test
    void testSequence1260() {
        Cube cube = new Cube(3, (x, y) -> {}, (x, y) -> {}, () -> {}, () -> {});
        String expected;

        try {
            expected = cube.show();

            for (int i = 0; i < 1260; i++) {
                cube.rotate(1, 0);
                cube.rotate(2, 2);
                cube.rotate(4, 0);
                cube.rotate(5, 1);
                cube.rotate(0, 0);
                cube.rotate(3, 1);
                cube.rotate(2, 2);
            }

            assertEquals(expected, cube.show());
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}