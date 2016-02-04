package ninja.smirking.events.bukkit;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * A collection of static utility methods that pertain to the handling of {@link Event} objects.
 *
 * @author Connor Spencer Harries
 * @version 1.0
 * @since 1.0
 */
public final class Events {
    private static final Logger internalLogger = Logger.getLogger(Events.class.getCanonicalName());

    private static Plugin plugin;

    /**
     * Register a {@link Listener} which handles an event of the given type once before unregistering itself.
     *
     * @param eventType non-null event type.
     * @param handler   non-null consumer that handles the event.
     * @param <T>       event type.
     * @return non-null Bukkit {@link Listener}.
     */
    public static <T extends Event> Listener observe(Class<T> eventType, Consumer<? super T> handler) {
        return observe(eventType, handler, EventPriority.NORMAL);
    }

    /**
     * Register a {@link Listener} which handles an event of the given type once before unregistering itself.
     *
     * @param eventType non-null event type.
     * @param handler   non-null consumer that handles the event.
     * @param priority  handler priority
     * @param <T>       event type.
     * @return non-null Bukkit {@link Listener}.
     */
    public static <T extends Event> Listener observe(Class<T> eventType, Consumer<? super T> handler, EventPriority priority) {
        return registerListener(eventType, (listener, event) -> {
            try {
                safeInvoke(eventType, event, handler);
            } finally {
                event.getHandlers().unregister(listener);
            }
        }, priority);
    }

    /**
     * Register a {@link Listener} which handles an event of the given type multiple times.
     * It is only unregistered when the providing plugin is disabled.
     *
     * @param eventType non-null event type.
     * @param handler   non-null consumer that handles the event.
     * @param <T>       event type.
     * @return non-null Bukkit {@link Listener}.
     */
    public static <T extends Event> Listener observeAll(Class<T> eventType, Consumer<? super T> handler) {
        return observeAll(eventType, handler, EventPriority.NORMAL);
    }

    /**
     * Register a {@link Listener} which handles an event of the given type multiple times.
     * It is only unregistered when the providing plugin is disabled.
     *
     * @param eventType non-null event type.
     * @param handler   non-null consumer that handles the event.
     * @param priority  handler priority
     * @param <T>       event type.
     * @return non-null Bukkit {@link Listener}.
     */
    public static <T extends Event> Listener observeAll(Class<T> eventType, Consumer<? super T> handler, EventPriority priority) {
        return registerListener(eventType, (listener, event) -> safeInvoke(eventType, event, handler), priority);
    }

    /**
     * Register a {@link Listener} which handles an event of the given type multiple times.
     * A {@link Predicate} controls which events are passed to the handler.
     *
     * @param eventType non-null event type.
     * @param handler   non-null consumer that handles the event.
     * @param test      non-null predicate that determines whether the event should be passed to the handler.
     * @param <T>       event type.
     * @return non-null Bukkit {@link Listener}.
     */
    public static <T extends Event> Listener observeIf(Class<T> eventType, Consumer<? super T> handler, Predicate<T> test) {
        return observeIf(eventType, handler, test, EventPriority.NORMAL);
    }

    /**
     * Register a {@link Listener} which handles an event of the given type multiple times.
     * A {@link Predicate} controls which events are passed to the handler.
     *
     * @param eventType non-null event type.
     * @param handler   non-null consumer that handles the event.
     * @param test      non-null predicate that determines whether the event should be passed to the handler.
     * @param priority  handler priority
     * @param <T>       event type.
     * @return non-null Bukkit {@link Listener}.
     */
    public static <T extends Event> Listener observeIf(Class<T> eventType, Consumer<? super T> handler, Predicate<T> test, EventPriority priority) {
        return registerListener(eventType, (listener, event) -> {
            if (test.test(event)) {
                safeInvoke(eventType, event, handler);
            }
        }, priority);
    }

    /**
     * Register a {@link Listener} which handles an event of the given type multiple times.
     * It is unregistered after the given duration has passed.
     *
     * @param eventType non-null event type.
     * @param handler   non-null consumer that handles the event.
     * @param duration  how long it should be before the {@link Listener} unregisters itself.
     * @param unit      the unit that the {@code duration} was given in.
     * @param <T>       event type.
     * @return non-null Bukkit {@link Listener}.
     */
    public static <T extends Event> Listener observeFor(Class<T> eventType, Consumer<? super T> handler, long duration, TimeUnit unit) {
        return observeFor(eventType, (event, time) -> handler.accept(event), duration, unit, EventPriority.NORMAL);
    }

    /**
     * Register a {@link Listener} which handles an event of the given type multiple times.
     * It is unregistered after the given duration has passed.
     *
     * @param eventType non-null event type.
     * @param handler   non-null consumer that handles the event and informs the handler of how many milliseconds are left before the {@link Listener} unregisters itself.
     * @param duration  how long it should be before the {@link Listener} unregisters itself.
     * @param unit      the unit that the {@code duration} was given in.
     * @param <T>       event type.
     * @return non-null Bukkit {@link Listener}.
     */
    public static <T extends Event> Listener observeFor(Class<T> eventType, BiConsumer<? super T, Long> handler, long duration, TimeUnit unit) {
        return observeFor(eventType, handler, duration, unit, EventPriority.NORMAL);
    }

    /**
     * Register a {@link Listener} which handles an event of the given type multiple times.
     * It is unregistered after the given duration has passed.
     *
     * @param eventType non-null event type.
     * @param handler   non-null consumer that handles the event and informs the handler of how many milliseconds are left before the {@link Listener} unregisters itself.
     * @param duration  how long it should be before the {@link Listener} unregisters itself.
     * @param unit      the unit that the {@code duration} was given in.
     * @param priority  handler priority
     * @param <T>       event type.
     * @return non-null Bukkit {@link Listener}.
     */
    public static <T extends Event> Listener observeFor(Class<T> eventType, BiConsumer<? super T, Long> handler, long duration, TimeUnit unit, EventPriority priority) {
        long deadline = Math.addExact(System.currentTimeMillis(), unit.toMillis(duration));
        return registerListener(eventType, (listener, event) -> {
            long invoked = System.currentTimeMillis();
            if (invoked > deadline) {
                event.getHandlers().unregister(listener);
            } else {
                safeInvoke(eventType, event, e -> handler.accept(e, deadline - invoked));
            }
        }, priority);
    }

    private static <T extends Event> Listener registerListener(Class<T> eventType, BiConsumer<Listener, ? super T> handler, EventPriority priority) {
        Preconditions.checkNotNull(eventType, "eventType");
        Preconditions.checkNotNull(handler, "handler");

        Listener listener = new Listener() {
        };
        //noinspection Convert2Lambda
        getPlugin().getServer().getPluginManager().registerEvent(eventType, listener, priority, new EventExecutor() {
            @Override
            public void execute(Listener listener, Event event) throws EventException {
                if (event.getClass() == eventType) {
                    handler.accept(listener, eventType.cast(event));
                }
            }
        }, plugin, false);
        return listener;
    }

    private static <T extends Event> void safeInvoke(Class<T> type, T event, Consumer<? super T> handler) {
        Preconditions.checkNotNull(handler, "handler cannot be null");
        Preconditions.checkNotNull(event, "event cannot be null");
        Preconditions.checkNotNull(type, "type cannot be null");
        try {
            handler.accept(event);
        } catch (Throwable cause) {
            trimStackTrace(cause);
            internalLogger.log(Level.INFO, "An unhandled exception was intercepted whilst handling {0}: \n{1}", new Object[]{
                    type.getName(), Throwables.getStackTraceAsString(cause)
            });
        }
    }

    private static Plugin getPlugin() {
        if (plugin == null) {
            plugin = JavaPlugin.getProvidingPlugin(Events.class);
        }
        return plugin;
    }

    private static void trimStackTrace(Throwable throwable) {
        List<StackTraceElement> elements = Lists.newArrayList(throwable.getStackTrace());
        for (Iterator<StackTraceElement> iterator = elements.iterator(); iterator.hasNext(); ) {
            StackTraceElement element = iterator.next();
            try {
                Class clazz = Class.forName(element.getClassName(), false, Thread.currentThread().getContextClassLoader());
                if (clazz == Events.class || (clazz.isAnonymousClass() && clazz.getEnclosingClass() == Events.class)) {
                    iterator.remove();
                }
            } catch (ClassNotFoundException ignored) {
                // $COVERAGE-IGNORE$
            }
        }
        throwable.setStackTrace(elements.toArray(new StackTraceElement[elements.size()]));
    }

    private Events() {
        throw new UnsupportedOperationException("Events cannot be instantiated!");
    }
}
