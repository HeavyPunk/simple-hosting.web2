package business.services.slickStorages

import components.basic.Monad
import slick.lifted.Rep

trait BaseStorage[TEntity, TTable, TEntityNotFoundError] {
    def add(item: TEntity): Monad[Exception, Boolean]
    def update(item: TEntity): Monad[Exception, Boolean]
    def find(predicate: TTable => Rep[Boolean]): Monad[Exception | TEntityNotFoundError, Seq[TEntity]]
    def remove(predicate: TTable => Rep[Boolean]): Monad[Exception, Boolean]
}
