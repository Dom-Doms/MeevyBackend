package it.domrita.meevy.chat.websocket;

import it.domrita.meevy.group.repository.GroupMembershipRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupMessageRecipientsService {

    private final GroupMembershipRepository groupMembershipRepository;

    @Transactional(
            readOnly = true,
            propagation = Propagation.REQUIRES_NEW
    )
    public List<Long> currentMemberIds(Long groupId) {

        // Rileggiamo i membri dopo il commit, non prima del salvataggio.
        return groupMembershipRepository
                .findMemberUserIdsByGroupId(groupId);
    }
}
