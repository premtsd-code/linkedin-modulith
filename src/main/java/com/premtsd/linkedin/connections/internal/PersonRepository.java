package com.premtsd.linkedin.connections.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

interface PersonRepository extends JpaRepository<Person, Long> {
    List<Person> findByUserIdIn(Collection<Long> userIds);
}
