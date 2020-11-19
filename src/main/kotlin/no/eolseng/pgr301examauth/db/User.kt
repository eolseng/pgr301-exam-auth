package no.eolseng.pgr301examauth.db

import no.eolseng.pgr301examauth.beer.Keg
import no.eolseng.pgr301examauth.beer.Mug
import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Entity
@Table(name = "USERS")
class User(

        @get:Id
        @get:NotBlank
        var username: String,

        @get:NotBlank
        var password: String,

        @get:ElementCollection
        @get:CollectionTable(
                name = "authorities",
                joinColumns = [JoinColumn(name = "username")]
        )
        @get:Column(name = "authority")
        @get:NotNull
        var roles: Set<String>? = setOf(),

        @get:NotNull
        var enabled: Boolean = true,

        @get:OneToMany(mappedBy = "owner", cascade = [(CascadeType.ALL)])
        var kegs : MutableSet<Keg>? = mutableSetOf(),

        @get:OneToMany(mappedBy = "owner", cascade = [(CascadeType.ALL)])
        var mugs : MutableSet<Mug>? = mutableSetOf()

)