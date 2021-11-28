package concurrentcube;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;


public class CubeTest {
    private final int REPETITION_NUM = 200;
    private final int TOP = 0;
    private final int LEFT = 1;
    private final int FRONT = 2;
    private final int RIGHT = 3;
    private final int BACK = 4;
    private final int BOTTOM = 5;
    private final int NUMBER_OF_SHOW_GROUP = 3;
    private final int NUMBER_OF_SIDES = 6;

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

    private void concurrentTest(int size, int numberOfThreads, int timeout, int interruptions) throws InterruptedException {
        AtomicInteger activeGroup = new AtomicInteger(0);
        AtomicInteger expectedGroup = new AtomicInteger(-1);
        AtomicBoolean collisionOfGroups = new AtomicBoolean(false);
        AtomicBoolean collisionOfLayers = new AtomicBoolean(false);
        boolean[][] layerUsed = new boolean[NUMBER_OF_SIDES / 2][size];
        ConcurrentLinkedQueue<List<Integer>> queue = new ConcurrentLinkedQueue<>();
        Semaphore mutex = new Semaphore(1, true);
        Thread[] threads = new Thread[numberOfThreads];

        Cube cube = new Cube(size,
                (side, layer) -> {
                    mutex.acquireUninterruptibly();

                    int axis = ((side + 2) % 5) % 3;
                    int layerIndex = Utils.getLayerIndex(side, layer, size);

                    if (expectedGroup.get() == -1) {
                        expectedGroup.set(axis);
                    }
                    else if (expectedGroup.get() != axis) {
                        collisionOfGroups.set(true);
                    }
                    else if (layerUsed[axis][layerIndex]) {
                        collisionOfLayers.set(true);
                    }

                    layerUsed[axis][layerIndex] = true;
                    activeGroup.incrementAndGet();

                    List<Integer> list = new ArrayList<>();
                    list.add(side);
                    list.add(layer);

                    queue.add(list);

                    mutex.release();
                },
                (side, layer) -> {
                    mutex.acquireUninterruptibly();

                    int axis = ((side + 2) % 5) % 3;
                    int layerIndex = Utils.getLayerIndex(side, layer, size);

                    layerUsed[axis][layerIndex] = false;

                    int active = activeGroup.decrementAndGet();

                    if (active == 0) {
                        expectedGroup.set(-1);
                    }

                    mutex.release();
                },
                () -> {
                    mutex.acquireUninterruptibly();

                    if (expectedGroup.get() == -1) {
                        expectedGroup.set(NUMBER_OF_SHOW_GROUP);
                    }
                    else if (expectedGroup.get() != NUMBER_OF_SHOW_GROUP) {
                        collisionOfGroups.set(true);
                    }

                    activeGroup.incrementAndGet();

                    mutex.release();
                },
                () -> {
                    mutex.acquireUninterruptibly();

                    int active = activeGroup.decrementAndGet();

                    if (active == 0) {
                        expectedGroup.set(-1);
                    }

                    mutex.release();
                }
        );

        for (int i = 0; i < numberOfThreads; i++) {
            double prob = ThreadLocalRandom.current().nextDouble();

            if (prob < 0.15) {
                threads[i] = new Thread(() -> {
                   try {
                       cube.show();
                   }
                   catch (InterruptedException ignored) {}
                });
            }
            else {
                int side = ThreadLocalRandom.current().nextInt(NUMBER_OF_SIDES);
                int layer = ThreadLocalRandom.current().nextInt(size);

                threads[i] = new Thread(() -> {
                    try {
                        cube.rotate(side, layer);
                    }
                    catch (InterruptedException ignored) {}
                });
            }
        }

        for (Thread t : threads) {
            t.start();
        }

        for (int i = numberOfThreads - 1; i >= (numberOfThreads - interruptions); i--) {
            threads[i].interrupt();
        }

        Assertions.assertTimeoutPreemptively(Duration.ofMillis(timeout), () -> {
            for (Thread t : threads) {
                t.join();
            }
        });

        Assertions.assertFalse(collisionOfGroups.get(), "Different groups of processes collided in critical section");
        Assertions.assertFalse(collisionOfLayers.get(), "One layer used by many processes");

        Assertions.assertTimeoutPreemptively(Duration.ofMillis(timeout), cube::show);

        Cube sequential = new Cube(size, (x, y) -> {}, (x, y) -> {}, () -> {}, () -> {});

        for (List<Integer> pair : queue) {
            sequential.rotate(pair.get(0), pair.get(1));
        }

        Assertions.assertEquals(sequential.show(), cube.show());
    }

    private String axisRotationsTestHelper(int side1, int side2) throws InterruptedException {
        final int SIZE = 5;
        Cube cube = new Cube(SIZE, (x, y) -> {}, (x, y) -> {}, () -> {}, () -> {});
        StringBuilder actual = new StringBuilder();

        for (int i = 0; i < SIZE; i++) {
            cube.rotate(side1, i);
            actual.append(cube.show()).append("\n");
            cube.rotate(side2, i);
            actual.append(cube.show()).append("\n");
        }

        return actual.toString();
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
        final int SIZE = 5;
        Cube cube = new Cube(SIZE, (x, y) -> {}, (x, y) -> {}, () -> {}, () -> {});
        StringBuilder actual = new StringBuilder();

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < NUMBER_OF_SIDES; j++) {
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
        final int SIZE = 10;
        Cube cube = new Cube(SIZE, (x, y) -> {}, (x, y) -> {}, () -> {}, () -> {});
        StringBuilder actual = new StringBuilder();

        for (int i = 0; i < NUMBER_OF_SIDES; i++) {
            for (int j = 0; j < SIZE; j++) {
                cube.rotate(i, j);
                actual.append(cube.show());
                actual.append("\n");
            }
        }

        assertEquals(getExpectedFromFile("sequentialRotationsTest10x10x10.txt"), actual.toString());
    }

    @Test
    @DisplayName("Sequential rotations around axis 0 test")
    public void axis0RotationsTest() throws InterruptedException, FileNotFoundException {
        String actual = axisRotationsTestHelper(LEFT, RIGHT);
        assertEquals(getExpectedFromFile("axis0RotationsTest.txt"), actual);
    }

    @Test
    @DisplayName("Sequential rotations around axis 1 test")
    public void axis1RotationsTest() throws InterruptedException, FileNotFoundException {
        String actual = axisRotationsTestHelper(FRONT, BACK);
        assertEquals(getExpectedFromFile("axis1RotationsTest.txt"), actual);
    }

    @Test
    @DisplayName("Sequential rotations around axis 2 test")
    public void axis2RotationsTest() throws InterruptedException, FileNotFoundException {
        String actual = axisRotationsTestHelper(TOP, BOTTOM);
        assertEquals(getExpectedFromFile("axis2RotationsTest.txt"), actual);
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
        final int SIZE = 3;
        Cube sequential = new Cube(SIZE, (x, y) -> {}, (x, y) -> {}, () -> {}, () -> {});
        Cube concurrent = new Cube(SIZE, (x, y) -> {}, (x, y) -> {}, () -> {}, () -> {});
        String expected;

        ExecutorService taskExecutorThreads = Executors.newFixedThreadPool(4);

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < SIZE; j++) {
                sequential.rotate(1, j);
            }
        }

        expected = sequential.show();

        for (int i = 0; i < 4; i++) {
            taskExecutorThreads.submit(() -> {
                for (int j = 0; j < SIZE; j++) {
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

    @RepeatedTest(REPETITION_NUM)
    @DisplayName("Concurrent test 1")
    public void concurrentTest1() throws InterruptedException {
        concurrentTest(10, 10, 1000, 0);
    }

    @RepeatedTest(REPETITION_NUM)
    @DisplayName("Concurrent test 2")
    public void concurrentTest2() throws InterruptedException {
        concurrentTest(50, 30, 2000, 0);
    }

    @RepeatedTest(REPETITION_NUM)
    @DisplayName("Concurrent test 3")
    public void concurrentTest3() throws InterruptedException {
        concurrentTest(100, 80, 3000, 0);
    }

    @RepeatedTest(REPETITION_NUM)
    @DisplayName("Interrupt test 1")
    public void interruptTest1() throws InterruptedException {
        concurrentTest(10, 10, 1000, 3);
    }

    @RepeatedTest(REPETITION_NUM)
    @DisplayName("Interrupt test 2")
    public void interruptTest2() throws InterruptedException {
        concurrentTest(50, 30, 2000, 10);
    }

    @RepeatedTest(REPETITION_NUM)
    @DisplayName("Interrupt test 3")
    public void interruptTest3() throws InterruptedException {
        concurrentTest(100, 80, 3000, 20);
    }
}
