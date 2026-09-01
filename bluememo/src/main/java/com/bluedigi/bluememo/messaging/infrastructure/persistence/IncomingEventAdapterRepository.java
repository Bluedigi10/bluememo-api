package com.bluedigi.bluememo.messaging.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bluedigi.bluememo.messaging.application.port.out.IncomingEventRepository;
import com.bluedigi.bluememo.messaging.domain.IncomingEvent;
import com.bluedigi.bluememo.messaging.infrastructure.persistence.entity.IncomingEventEntity;
import com.bluedigi.bluememo.messaging.infrastructure.persistence.mapper.IncomingEventEntityMapper;
import com.bluedigi.bluememo.messaging.infrastructure.persistence.repository.IncomingEventJpaRepository;

@Repository
public class IncomingEventAdapterRepository implements IncomingEventRepository {
    private final IncomingEventJpaRepository jpaRepository;
    private final IncomingEventEntityMapper entityMapper;

    public IncomingEventAdapterRepository(IncomingEventJpaRepository jpaRepository, IncomingEventEntityMapper entityMapper) {
        this.jpaRepository = jpaRepository;
        this.entityMapper = entityMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Boolean insertIfAbsent(IncomingEvent incomingEvent) {
        IncomingEventEntity entity = entityMapper.toEntity(incomingEvent);

        return jpaRepository.insertIfAbsent(entity) == 1;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Boolean updateIncomingEventStatus(IncomingEvent incomingEvent) {
        return jpaRepository.updateStatusByExternalEventIdAndChannelType(
            incomingEvent.getExternalEventId(),
            incomingEvent.getChannelType(),
            incomingEvent.getStatus(),
            incomingEvent.getProcessedAt()) == 1;
    }

}
