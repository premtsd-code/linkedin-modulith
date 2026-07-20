/**
 * Feed module — materializes each author's post into their connections' feeds. The one
 * workload here that genuinely cannot be a one-shot event listener: a well-connected
 * author's post touches hundreds of thousands of rows, so fanout runs on the {@code jobs}
 * runtime (chunked, checkpointed, resumable) rather than inline in an event handler.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Feed",
        allowedDependencies = {"shared", "jobs", "post::events", "connections"})
package com.premtsd.linkedin.feed;
