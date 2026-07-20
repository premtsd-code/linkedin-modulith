@org.springframework.modulith.ApplicationModule(
        displayName = "Notification",
        allowedDependencies = {"shared", "user::events", "post::events", "connections::events"})
package com.premtsd.linkedin.notification;
