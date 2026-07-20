package com.premtsd.linkedin.connections;

import java.util.List;

/**
 * Exposed read API of the connections module for high-volume consumers (feed fanout).
 * Keyset pagination — {@code afterId} is the last id from the previous page — because
 * fanout to a large connection set is exactly where OFFSET paging degrades.
 */
public interface ConnectionGraphQuery {

    /**
     * Accepted-connection user ids of {@code userId}, strictly greater than {@code afterId},
     * ascending, capped at {@code limit}. Start a scan with {@code afterId = 0}.
     */
    List<Long> followerIdsAfter(Long userId, long afterId, int limit);
}
