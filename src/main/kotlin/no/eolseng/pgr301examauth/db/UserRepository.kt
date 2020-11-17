package no.eolseng.pgr301examauth.db

import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, String>