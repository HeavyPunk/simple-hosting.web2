package business.entities

import components.basic.{
    Monad,
    ResultMonad
}

trait Observator[TGetErrors, TModel] {
    def get: Monad[TGetErrors, TModel]
}

class DatabaseObservator[TGetErrors, TModel](getter: () => Monad[TGetErrors, TModel]) extends Observator[TGetErrors, TModel]
{
    var got: Monad[TGetErrors, TModel] = null.asInstanceOf[Monad[TGetErrors, TModel]]
    override def get: Monad[TGetErrors, TModel] = {
        if (got == null){
            got = getter()
            got
        } else got
    }
}

class ObjectObservator[TGetErrors, TModel](obj: TModel) extends Observator[TGetErrors, TModel]
{
    val cached: Monad[TGetErrors, TModel] = ResultMonad(obj)
    override def get: Monad[TGetErrors, TModel] = cached
}
