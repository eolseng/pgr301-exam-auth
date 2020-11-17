package no.eolseng.pgr301examauth.db

import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
@Transactional
class UserService(
        private val repository: UserRepository,
        private val userDetailsService: UserDetailsServiceImpl,
        private val authManager: AuthenticationManager,
        private val passwordEncoder: PasswordEncoder
) {

    fun createUser(
            username: String,
            password: String,
            roles: Set<String> = setOf()
    ): Boolean {
        try {

            val hash = passwordEncoder.encode(password)
            val caseInsensitiveUsername = username.toLowerCase()

            if (repository.existsById(caseInsensitiveUsername)) return false
            val user = User(
                    username = caseInsensitiveUsername,
                    password = hash,
                    roles = roles.map { "ROLE_$it" }.toSet()
            )
            repository.save(user)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun deleteUser(
            username: String,
            password: String
    ): Boolean {

        // Attempt to retrieve the user from database
        val userDetails = try {
            userDetailsService.loadUserByUsername(username)
        } catch (e: UsernameNotFoundException) {
            return false
        }

        // Attempt to authenticate user with username and password
        val token = UsernamePasswordAuthenticationToken(userDetails, password, userDetails.authorities)
        authManager.authenticate(token)

        // Check if password is correct
        if (!token.isAuthenticated) return false

        // Delete the user
        repository.deleteById(username)
        return true

    }

}