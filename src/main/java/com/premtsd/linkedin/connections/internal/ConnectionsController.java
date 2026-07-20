package com.premtsd.linkedin.connections.internal;

import com.premtsd.linkedin.shared.SecurityUtils;
import com.premtsd.linkedin.connections.internal.ConnectionsDtos.PersonView;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/connections")
@RequiredArgsConstructor
class ConnectionsController {

    private final ConnectionsService connectionsService;

    @PostMapping("/request/{receiverId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendRequest(@PathVariable Long receiverId) {
        connectionsService.sendRequest(SecurityUtils.currentUserId(), receiverId);
    }

    @PostMapping("/accept/{senderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(@PathVariable Long senderId) {
        connectionsService.acceptRequest(SecurityUtils.currentUserId(), senderId);
    }

    @GetMapping("/first-degree")
    public List<PersonView> firstDegree() {
        return connectionsService.firstDegree(SecurityUtils.currentUserId());
    }
}
