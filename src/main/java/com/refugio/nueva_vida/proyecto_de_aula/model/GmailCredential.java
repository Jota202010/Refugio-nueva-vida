package com.refugio.nueva_vida.proyecto_de_aula.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "gmail_credential")
public class GmailCredential {

    @Id
    private Integer id;

    @Column(nullable = false, length = 150)
    private String email;

    @Lob
    @Column(nullable = false)
    private String encryptedRefreshToken;

    protected GmailCredential() {
    }

    public GmailCredential(Integer id, String email, String encryptedRefreshToken) {
        this.id = id;
        this.email = email;
        this.encryptedRefreshToken = encryptedRefreshToken;
    }

    public Integer getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getEncryptedRefreshToken() {
        return encryptedRefreshToken;
    }
}
