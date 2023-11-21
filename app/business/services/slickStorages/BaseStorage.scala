package business.services.slickStorages

import components.basic.Monad
import slick.lifted.Rep

trait BaseStorage[TEntity, TTable, TAddError, TUpdateError, TFindError, TRemoveError] {
    def create(modifier: TEntity => Unit = null): TEntity
    def add(item: TEntity): Monad[TAddError, Boolean]
    def update(item: TEntity): Monad[TUpdateError, Boolean]
    def find(predicate: TTable => Rep[Boolean]): Monad[TFindError, Seq[TEntity]]
    def remove(predicate: TTable => Rep[Boolean]): Monad[TRemoveError, Boolean]
}
