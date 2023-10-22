package business.services.storages.userGroups

import jakarta.persistence.EntityManager
import components.services.log.Log
import business.services.storages.BaseStorage
import business.entities.UserGroup
import components.basic.{
    Monad,
    ErrorMonad,
    ResultMonad
}

class UserGroupNotFound
class GroupNotFound(val name: String)

class UserGroupsStorage(
    val em: EntityManager,
    val logger: Log
) extends BaseStorage[UserGroup] {
    override val entityManager: EntityManager = em
    override val log: Log = logger

    def findGroupById[TKey](id: TKey): Monad[Exception | UserGroupNotFound, UserGroup] = 
        val result = findById(id)
        result match
            case r: ErrorMonad[?, ?] => ErrorMonad(r.err)
            case r: ResultMonad[?, ?] => if r.obj != null then ResultMonad(r.obj) else ErrorMonad(UserGroupNotFound())

    def findByName(name: String): Monad[GroupNotFound | Exception, UserGroup] =
        val result = query("from UserGroup where name=:name",
            "name" -> name
        )
        result match
            case r: ErrorMonad[?, ?] => ErrorMonad(r.err)
            case r: ResultMonad[?, ?] => if r.obj.length == 0 then ErrorMonad(GroupNotFound(name)) else ResultMonad(r.obj(0))
}
