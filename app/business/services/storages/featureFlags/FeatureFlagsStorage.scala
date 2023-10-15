package business.services.storages.featureFlags

import business.services.storages.BaseStorage
import business.entities.FeatureFlag
import jakarta.persistence.EntityManager
import components.services.log.Log
import components.basic.Monad
import components.basic.ErrorMonad
import components.basic.ResultMonad

class FeatureFlagNotFound

class FeatureFlagsStorage(
    em: EntityManager,
    logger: Log
) extends BaseStorage[FeatureFlag] {
    override val entityManager: EntityManager = em
    override val log: Log = logger

    def findByName(name: String): Monad[FeatureFlagNotFound | Exception, FeatureFlag] =
        val result = query(
            "from FeatureFlag where name=:name",
            "name" -> name
        )
        result match
            case r: ErrorMonad[?, ?] => ErrorMonad(r.err)
            case r: ResultMonad[?, ?] => if r.obj.length == 0 then ErrorMonad(FeatureFlagNotFound()) else ResultMonad(r.obj(0))
}
