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

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;


public class CubeTest {
    private final int REPETITION_NUM = 50;

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
    public void repeatedSequenceTest() throws InterruptedException {
        //1260 sequences are necessary to get back to the original position of 3x3x3 Rubik's Cube.
        //https://math.stackexchange.com/questions/2279155/period-for-a-rubiks-cube-repeated-manipulation

        Cube cube = new Cube(3, (x, y) -> {}, (x, y) -> {}, () -> {}, () -> {});
        String expected;

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

    @RepeatedTest(REPETITION_NUM)
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
