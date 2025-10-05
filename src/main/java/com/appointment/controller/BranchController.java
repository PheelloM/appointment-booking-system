package com.appointment.controller;

import com.appointment.entity.Branch;
import com.appointment.repository.BranchRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/branches")
public class BranchController {
    private final BranchRepository branchRepository;

    public BranchController(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    @GetMapping
    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }
}
