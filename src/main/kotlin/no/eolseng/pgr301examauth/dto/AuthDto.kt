package no.eolseng.pgr301examauth.dto

import javax.validation.constraints.NotBlank

data class AuthDto(

        @get:NotBlank
        var username: String,

        @get:NotBlank
        val password: String

)