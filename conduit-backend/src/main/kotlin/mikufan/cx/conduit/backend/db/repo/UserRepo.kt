package mikufan.cx.conduit.backend.db.repo

import mikufan.cx.conduit.backend.db.User
import mikufan.cx.conduit.backend.db.Users
import mikufan.cx.conduit.common.UserRegisterDto
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere

class UserRepo {

  fun getById(id: Int): User? = User.findById(id)

  fun getByEmail(email: String): User? = User.find { Users.email eq email }.firstOrNull()

  fun getByUsername(username: String): User? = User.find { Users.username eq username }.firstOrNull()

  fun insert(userRegisterDto: UserRegisterDto): User = User.new {
    email = userRegisterDto.email
    username = userRegisterDto.username
    password = userRegisterDto.password
    bio = ""
    image = null
  }

  fun delete(id: Int): Boolean = Users.deleteWhere { Users.id eq id } > 0

}