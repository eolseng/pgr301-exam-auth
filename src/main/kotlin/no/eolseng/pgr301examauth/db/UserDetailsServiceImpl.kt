package no.eolseng.pgr301examauth.db

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class UserDetailsServiceImpl(
        private val userRepository: UserRepository
) : UserDetailsService {

    @Transactional
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository
                .findById(username)
                .orElseThrow {
                    throw UsernameNotFoundException("No user with username $username found.")
                }
        val authorities = user.roles!!.map { GrantedAuthority { it } }
        return User(user.username, user.password, authorities)
    }

}