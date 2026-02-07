package mikufan.cx.conduit.backend.db

import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object Users : IntIdTable() {
  val email = varchar("email", 255).uniqueIndex()
  val username = varchar("username", 255).uniqueIndex()
  val password = varchar("password", 255)
  val bio = varchar("bio", 255).nullable()
  val image = varchar("image", 255).nullable()
}

class User(id: EntityID<Int>) : IntEntity(id) {
  companion object : IntEntityClass<User>(Users)

  var email by Users.email
  var username by Users.username
  var password by Users.password
  var bio by Users.bio
  var image by Users.image
}