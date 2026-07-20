@org.springframework.modulith.ApplicationModule(
        displayName = "Notification",
        allowedDependencies = {"shared", "jobs", "user::events", "post::events", "connections::events"})
package com.premtsd.linkedin.notification;
