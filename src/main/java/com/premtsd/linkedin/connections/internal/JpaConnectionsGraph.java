package com.premtsd.linkedin.connections.internal;

import com.premtsd.linkedin.connections.internal.ConnectionRequest.Status;
import com.premtsd.linkedin.connections.internal.ConnectionsDtos.PersonView;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Relational adapter (default). Active unless the 'neo4j' profile is on. */
@Repository
@Profile("!neo4j")
@RequiredArgsConstructor
class JpaConnectionsGraph implements ConnectionsGraph {

    private final ConnectionRequestRepository requests;
    private final PersonRepository persons;

    @Override
    public void registerPerson(Long userId, String name) {
        if (!persons.existsById(userId)) {
            persons.save(Person.builder().userId(userId).name(name).build());
        }
    }

    @Override
    public boolean relationshipExists(Long a, Long b) {
        return requests.existsBySenderIdAndReceiverId(a, b)
                || requests.existsBySenderIdAndReceiverId(b, a);
    }

    @Override
    public void createPendingRequest(Long senderId, Long receiverId) {
        requests.save(ConnectionRequest.builder()
                .senderId(senderId).receiverId(receiverId).status(Status.PENDING).build());
    }

    @Override
    public boolean markAccepted(Long senderId, Long receiverId) {
        Optional<ConnectionRequest> opt = requests.findBySenderIdAndReceiverId(senderId, receiverId);
        if (opt.isEmpty() || opt.get().getStatus() == Status.ACCEPTED) {
            return false;
        }
        ConnectionRequest req = opt.get();
        req.setStatus(Status.ACCEPTED);
        requests.save(req);
        return true;
    }

    @Override
    public List<PersonView> firstDegree(Long userId) {
        List<Long> ids = requests.findConnectedUserIds(userId, Status.ACCEPTED);
        return persons.findByUserIdIn(ids).stream()
                .map(p -> new PersonView(p.getUserId(), p.getName()))
                .toList();
    }
}
