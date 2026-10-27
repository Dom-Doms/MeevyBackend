package it.domrita.meevy.subject.controller;

import it.domrita.meevy.subject.dto.SubjectResponse;
import it.domrita.meevy.subject.dto.SubjectRequest;
import it.domrita.meevy.subject.service.SubjectService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/subject")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @PostMapping
    public ResponseEntity<SubjectResponse> createSubject(
            @Valid @RequestBody SubjectRequest subjectRequest) {

        SubjectResponse subjectResponse = subjectService.createSubject(subjectRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(subjectResponse);
    }
}
