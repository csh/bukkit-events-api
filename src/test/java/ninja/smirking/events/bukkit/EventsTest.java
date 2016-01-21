package ninja.smirking.events.bukkit;

import java.lang.reflect.Constructor;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;

/**
 * Simple tests that confirm {@link Events} is working as intended.
 *
 * @author Connor Spencer Harries
 * @version 1.0
 * @since 1.0
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({JavaPlugin.class})
public final class EventsTest {
    private static JavaPlugin plugin;
    private static Server server;

    @Rule
    private final TestName testName = new TestName();

    @BeforeClass
    public static void tinker() throws Exception {
        server = new MockServer();
        PowerMockito.mockStatic(JavaPlugin.class);

        plugin = new MockPlugin(server);
        Mockito.when(JavaPlugin.getProvidingPlugin(Matchers.eq(Events.class))).thenReturn(plugin);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void satisfyCoverage() throws Exception {
        Constructor<Events> constructor = Events.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    public void testObserve() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        Events.observe(DummyEvent.class, event -> increment(counter));
        server.getPluginManager().callEvent(new DummyEvent());
        server.getPluginManager().callEvent(new DummyEvent());
        assertEquals("Event handler should have been fired once", 1, counter.get());
    }

    private void increment(AtomicInteger counter) {
        counter.incrementAndGet();
        log("Incrementing counter");
    }

    private void log(String message) {
        System.out.printf("[%s] %s%n", testName.getMethodName(), message);
    }

    @Test
    public void testObserveAll() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        Events.observeAll(DummyEvent.class, event -> increment(counter));
        server.getPluginManager().callEvent(new DummyEvent());
        server.getPluginManager().callEvent(new DummyEvent());
        assertEquals("Event handler should have been fired twice", 2, counter.get());
    }

    @Test
    public void testObserveIf() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        Events.observeIf(DummyEvent.class, event -> increment(counter), event -> event.getMessage().charAt(0) == 'H');
        server.getPluginManager().callEvent(new DummyEvent("Hello World"));
        server.getPluginManager().callEvent(new DummyEvent("Do you like waffles?"));
        assertEquals("Event handler should have been fired once", 1, counter.get());
    }

    @Test
    public void testObserveFor() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        Events.observeFor(DummyEvent.class, event -> increment(counter), 3L, TimeUnit.SECONDS);
        server.getPluginManager().callEvent(new DummyEvent());
        Thread.sleep(TimeUnit.SECONDS.toMillis(3L));
        server.getPluginManager().callEvent(new DummyEvent());
        assertEquals("Event handler should have been fired once", 1, counter.get());
    }

    @Test
    public void testObserveFor1() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        Events.observeFor(DummyEvent.class, (event, remaining) -> {
            increment(counter);
            log(TimeUnit.MILLISECONDS.toSeconds(remaining) + " seconds left");
        }, 3L, TimeUnit.SECONDS);
        server.getPluginManager().callEvent(new DummyEvent());
        Thread.sleep(TimeUnit.SECONDS.toMillis(3L));
        server.getPluginManager().callEvent(new DummyEvent());
        assertEquals("Event handler should have been fired once", 1, counter.get());
    }

    @Test
    public void testBenchmark() throws Exception {
        if (Boolean.getBoolean("benchmark")) {
            long nanoseconds = System.currentTimeMillis();
            System.out.println(nanoseconds);
            Events.observeIf(DummyEvent.class, new Consumer<DummyEvent>() {
                private int doots = 0;

                @Override
                public void accept(DummyEvent dummyEvent) {
                    doots++;
                }

                @Override
                public String toString() {
                    return String.valueOf(doots);
                }
            }, event -> event.getMessage().length() > 5);

            for (int i = 0; i < 100000; i++) {
                server.getPluginManager().callEvent(new DummyEvent(UUID.randomUUID().toString()));
            }
            System.out.println(System.currentTimeMillis() - nanoseconds);
        }
    }

    @Test
    public void testExceptionHandling() throws Exception {
        Events.observe(DummyEvent.class, event -> {
            throw new RuntimeException("All your base are belong to us");
        });
        server.getPluginManager().callEvent(new DummyEvent());
    }

    @After
    public void after() throws Exception {
        HandlerList.unregisterAll(plugin);
    }

    static class DummyEvent extends Event {
        private static final HandlerList handlers = new HandlerList();

        private final String message;

        public DummyEvent() {
            this("");
        }

        private DummyEvent(String message) {
            this.message = message;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public HandlerList getHandlers() {
            return handlers;
        }
    }
}