package com.transporter.assignment.repository;

import com.transporter.assignment.entity.Transporter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransporterRepository extends JpaRepository<Transporter, Long> {
}
