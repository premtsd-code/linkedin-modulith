package com.premtsd.linkedin.platform.persistence;

import java.util.function.Supplier;

/**
 * Read-your-writes control for the read/write-split datasources.
 *
 * <p>By default a {@code @Transactional(readOnly = true)} read goes to a replica (see
 * {@link ReadReplicaRouting}), which replicates asynchronously and may be a little behind the
 * primary. That is fine for most reads (feeds, notification lists), but a read that must
 * observe a write the same user just made can miss it on a lagging replica.
 *
 * <p>Wrap such a read in {@link #fromPrimary} to pin the read-only transactions on this thread
 * to the primary (which always has the latest committed data) for the duration of the call —
 * without dropping the {@code readOnly} flag. Use it only on the few read-after-write-sensitive
 * paths; pinning everything to the primary would throw away the replica offload.
 *
 * <pre>{@code
 *   // just after saving a profile edit, show the fresh value:
 *   var profile = RoutingHints.fromPrimary(() -> profileService.get(userId));
 * }</pre>
 *
 * <p>Off the 'feeddb' profile there is a single database and nothing consults this hint, so the
 * calls are harmless no-ops.
 */
public final class RoutingHints {

    private static final ThreadLocal<Boolean> FORCE_PRIMARY = new ThreadLocal<>();

    private RoutingHints() {
    }

    static boolean isPrimaryForced() {
        return Boolean.TRUE.equals(FORCE_PRIMARY.get());
    }

    /** Run {@code read} with read-only transactions on this thread pinned to the primary. */
    public static <T> T fromPrimary(Supplier<T> read) {
        boolean alreadyForced = isPrimaryForced();
        FORCE_PRIMARY.set(Boolean.TRUE);
        try {
            return read.get();
        } finally {
            if (alreadyForced) {
                FORCE_PRIMARY.set(Boolean.TRUE);   // keep an outer scope's pin intact
            } else {
                FORCE_PRIMARY.remove();
            }
        }
    }

    /** {@link #fromPrimary(Supplier)} for a read with no return value. */
    public static void fromPrimary(Runnable read) {
        fromPrimary(() -> {
            read.run();
            return null;
        });
    }

    /**
     * Pin every read-only transaction on this thread to the primary until {@link #clear()}.
     * For request-scoped bracketing (a servlet filter / MVC interceptor) where a try/finally
     * lambda does not fit; the caller MUST pair it with {@link #clear()} in a finally, or the
     * flag leaks onto the next request that reuses this pooled thread. Prefer
     * {@link #fromPrimary(Supplier)} for ordinary in-method use.
     */
    public static void pinToPrimary() {
        FORCE_PRIMARY.set(Boolean.TRUE);
    }

    /** Remove any pin on this thread. Safe to call when none is set. */
    public static void clear() {
        FORCE_PRIMARY.remove();
    }
}
