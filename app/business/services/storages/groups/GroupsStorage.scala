package business.services.storages.groups

import business.services.storages.BaseStorage
import business.entities.UserGroup
import components.services.log.Log
import jakarta.persistence.EntityManager
import components.basic.Monad
import org.checkerframework.checker.units.qual.m
import components.basic.ErrorMonad
import components.basic.ResultMonad

class GroupNotFound(val name: String)

class GroupsStorage(
    em: EntityManager,
    logger: Log
) extends BaseStorage[UserGroup] {
    override val entityManager: EntityManager = em
    override val log: Log = logger

    def findByName(name: String): Monad[GroupNotFound | Exception, UserGroup] =
        val result = query("from UserGroup where name=:name",
            "name" -> name
        )
        result match
            case r: ErrorMonad[?, ?] => ErrorMonad(r.err)
            case r: ResultMonad[?, ?] => if r.obj.length == 0 then ErrorMonad(GroupNotFound(name)) else ResultMonad(r.obj(0))
        
}
