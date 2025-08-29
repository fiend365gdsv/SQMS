package com.sqms.sqms_backend.doctor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@CrossOrigin(origins = "*")
public class DoctorController {

    private final DoctorRepository doctorRepo;

    public DoctorController(DoctorRepository doctorRepo) {
        this.doctorRepo = doctorRepo;
    }

    @PostMapping("/{id}/availability")
    public Doctor updateAvailability(@PathVariable Long id, @RequestParam boolean available) {
        Doctor doctor = doctorRepo.findById(id).orElseThrow(() ->
                new IllegalArgumentException("Doctor not found"));
        doctor.setAvailable(available);
        return doctorRepo.save(doctor);
    }

    @GetMapping
    public List<Doctor> getAllDoctors() {
        return doctorRepo.findAll();
    }
}
