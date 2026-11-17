package it.domrita.meevy.chat.controller;

import it.domrita.meevy.chat.dto.GroupMessageRequest;
import it.domrita.meevy.chat.dto.GroupMessageResponse;
import it.domrita.meevy.chat.service.GroupMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/groups/{groupId}/messages")
@RequiredArgsConstructor
public class GroupMessageController {

    private final GroupMessageService groupMessageService;

    // senderUserId viene dal token: il client manda solo il testo.
    @PostMapping
    public ResponseEntity<GroupMessageResponse> sendMessage(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupMessageRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupMessageService.sendMessage(groupId, request));
    }

    @GetMapping
    public ResponseEntity<List<GroupMessageResponse>> getMessages(
            @PathVariable Long groupId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "30") int limit
    ) {
        return ResponseEntity.ok(
                groupMessageService.getMessages(groupId, beforeId, limit)
        );
    }
}