package concurrentcube;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;


public class CubeTest {
    private final int REPETITION_NUM = 100;

    private String getExpectedFromFile(String fileName) throws FileNotFoundException {
        StringBuilder expected = new StringBuilder();
        File file = Paths.get("src", "concurrentcube", "tests", fileName).toFile();
        Scanner reader = new Scanner(file);

        while (reader.hasNextLine()) {
            expected.append(reader.nextLine()).append("\n");
        }

        reader.close();

        return expected.toString();
    }

    @Test
    @DisplayName("Initialization test")
    public void initializationTest() throws InterruptedException, FileNotFoundException {
        Cube cube = new Cube(10, (x, y) -> {}, (x, y) -> {}, () -> {}, () -> {});
        String actual = cube.show() + "\n";
        assertEquals(getExpectedFromFile("initializationTest.txt"), actual);
    }

    @Test
    @DisplayName("Sequential rotations test on 5x5x5 cube")
    public void sequentialRotationsTest5x5x5() throws InterruptedException, FileNotFoundException {
        Cube cube = new Cube(5, (x, y) -> {}, (x, y) -> {}, () -> {}, () -> {});
        StringBuilder actual = new StringBuilder();

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 6; j++) {
                cube.rotate(j, i);
                actual.append(cube.show());
                actual.append("\n");
            }
        }

        assertEquals(getExpectedFromFile("sequentialRotationsTest5x5x5.txt"), actual.toString());
    }

    @Test
    @DisplayName("Sequential rotations test on 10x10x10 cube")
    public void sequentialRotationsTest10x10x10() throws InterruptedException, FileNotFoundException {
        Cube cube = new Cube(10, (x, y) -> {}, (x, y) -> {}, () -> {}, () -> {});
        StringBuilder actual = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 10; j++) {
                cube.rotate(i, j);
                actual.append(cube.show());
                actual.append("\n");
            }
        }

        assertEquals(getExpectedFromFile("sequentialRotationsTest10x10x10.txt"), actual.toString());
    }

    @Test
    @DisplayName("Sequential rotations around axis 0 test on 5x5x5 cube")
    public void axis0RotationsTest() throws InterruptedException, FileNotFoundException {
        Cube cube = new Cube(5, (x, y) -> {}, (x, y) -> {}, () -> {}, () -> {});
        StringBuilder actual = new StringBuilder();

        for (int i = 0; i < 5; i++) {
            cube.rotate(1, i);
            actual.append(cube.show()).append("\n");
            cube.rotate(3, i);
            actual.append(cube.show()).append("\n");
        }

        assertEquals(getExpectedFromFile("axis0RotationsTest.txt"), actual.toString());
    }

    @Test
    @DisplayName("Sequential rotations around axis 1 test on 5x5x5 cube")
    public void axis1RotationsTest() throws InterruptedException, FileNotFoundException {
        Cube cube = new Cube(5, (x, y) -> {}, (x, y) -> {}, () -> {}, () -> {});
        StringBuilder actual = new StringBuilder();

        for (int i = 0; i < 5; i++) {
            cube.rotate(2, i);
            actual.append(cube.show()).append("\n");
            cube.rotate(4, i);
            actual.append(cube.show()).append("\n");
        }

        assertEquals(getExpectedFromFile("axis1RotationsTest.txt"), actual.toString());
    }

    @Test
    @DisplayName("Sequential rotations around axis 2 test on 5x5x5 cube")
    public void axis2RotationsTest() throws InterruptedException, FileNotFoundException {
        Cube cube = new Cube(5, (x, y) -> {}, (x, y) -> {}, () -> {}, () -> {});
        StringBuilder actual = new StringBuilder();

        for (int i = 0; i < 5; i++) {
            cube.rotate(0, i);
            actual.append(cube.show()).append("\n");
            cube.rotate(5, i);
            actual.append(cube.show()).append("\n");
        }

        assertEquals(getExpectedFromFile("axis2RotationsTest.txt"), actual.toString());
    }

    @Test
    @DisplayName("Repeated sequence test")
    public void repeatedSequenceTest() throws InterruptedException {
        //1260 repetitions of the same sequence are necessary to get back to the initial state of 3x3x3 Rubik's Cube.
        //https://math.stackexchange.com/questions/2279155/period-for-a-rubiks-cube-repeated-manipulation

        Cube cube = new Cube(3, (x, y) -> {}, (x, y) -> {}, () -> {}, () -> {});
        String expected = cube.show();

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

    @RepeatedTest(REPETITION_NUM)
    @DisplayName("Simple concurrent rotations test")
    public void simpleConcurrentTest() throws InterruptedException {
        Cube sequential = new Cube(3, (x, y) -> {}, (x, y) -> {}, () -> {}, () -> {});
        Cube concurrent = new Cube(3, (x, y) -> {}, (x, y) -> {}, () -> {}, () -> {});
        String expected;

        ExecutorService taskExecutorThreads = Executors.newFixedThreadPool(4);

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 3; j++) {
                sequential.rotate(1, j);
            }
        }

        expected = sequential.show();

        for (int i = 0; i < 4; i++) {
            taskExecutorThreads.submit(() -> {
                for (int j = 0; j < 3; j++) {
                    try {
                        concurrent.rotate(1, j);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        taskExecutorThreads.shutdown();
        assertTrue(taskExecutorThreads.awaitTermination(1000, TimeUnit.MILLISECONDS));
        assertEquals(expected, concurrent.show());
    }
}
