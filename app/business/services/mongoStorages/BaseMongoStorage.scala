package business.services.mongoStorages

import components.basic.Monad
import org.bson.conversions.Bson

trait BaseMongoStorage[TEntity] {
    def add(entity: TEntity): Monad[Exception, Boolean]
    def update(entity: TEntity): Monad[Exception, Boolean]
    def addOrUpdate(entity: TEntity): Monad[Exception, Boolean]
    def find(predicate: Bson): Monad[Exception, Seq[TEntity]]
    def remove(predicate: Bson = null): Monad[Exception, Boolean]
}
