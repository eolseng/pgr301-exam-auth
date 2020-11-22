package no.eolseng.pgr301examauth.config

import no.eolseng.pgr301examauth.db.UserDetailsServiceImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.config.web.servlet.invoke
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler

@Configuration
@EnableWebSecurity
class WebSecurityConfig(
        private val userDetailsService: UserDetailsServiceImpl
) : WebSecurityConfigurerAdapter() {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    override fun authenticationManagerBean(): AuthenticationManager {
        return super.authenticationManagerBean()
    }

    override fun configure(auth: AuthenticationManagerBuilder) {
        auth
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder())
    }

    override fun configure(http: HttpSecurity) {
        http {
            httpBasic {}
            csrf { disable() }
            logout {
                logoutUrl = "/api/v1/auth/logout"
                invalidateHttpSession = true
                deleteCookies("JSESSIONID")
                logoutSuccessHandler = HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)
            }
            authorizeRequests {
                authorize("/api/v1/auth/signup", permitAll)
                authorize("/api/v1/auth/login", permitAll)
                authorize("/api/v1/auth/logout", permitAll)
                authorize("/api/v1/auth/user", authenticated)

                authorize("/api/v1/keg/**", authenticated)
                authorize("/api/v1/mug/**", authenticated)
                authorize("/api/v1/brew/**", authenticated)
                authorize("/api/v1/tap/**", authenticated)
                authorize("/api/v1/sip/**", authenticated)

                // TODO: Should all actuator endpoints be exposed?
                authorize("/actuator/**", permitAll)

                authorize(anyRequest, denyAll)
            }
            exceptionHandling {
                // Stops 401-responses from triggering a WWW-authenticate header that triggers basic login form
                authenticationEntryPoint = HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
            }
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.IF_REQUIRED
            }
        }
    }

}