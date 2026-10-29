package it.domrita.meevy.subject.controller;

import it.domrita.meevy.subject.dto.SubjectResponse;
import it.domrita.meevy.subject.dto.SubjectRequest;
import it.domrita.meevy.subject.service.SubjectService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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

    @GetMapping
    public ResponseEntity<List<SubjectResponse>> getAllSubjects() {

        List<SubjectResponse> subject = subjectService.getAllSubjects();

        return ResponseEntity          //risosta HTTP che invierà al Client
                .status(HttpStatus.OK) // codice HTTP 200 OK
                .body(subject);        //inserisce la lista recuperata dal Service
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubjectResponse> getSubjectById(
            @PathVariable Long id
    ){
        SubjectResponse subject = subjectService.getSubjectById(id);

        return ResponseEntity.status(HttpStatus.OK).body(subject);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubjectResponse> updateSubject(
            @PathVariable Long id,
            @Valid @RequestBody SubjectRequest subjectRequest
    ){
        SubjectResponse subject = subjectService.updateSubject(id, subjectRequest);

        return ResponseEntity.status(HttpStatus.OK).body(subject);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<SubjectResponse> deleteSubject(
            @PathVariable Long id
    ){
        subjectService.deleteSubject(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
