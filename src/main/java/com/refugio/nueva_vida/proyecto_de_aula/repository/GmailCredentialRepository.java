package com.refugio.nueva_vida.proyecto_de_aula.repository;

import com.refugio.nueva_vida.proyecto_de_aula.model.GmailCredential;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GmailCredentialRepository extends JpaRepository<GmailCredential, Integer> {
}
