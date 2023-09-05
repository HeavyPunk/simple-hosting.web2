package components.basic

import scala.annotation.retains

extension [E, A](monad: Monad[E, A]) //library extensions
    def zipWith[E1, A1](other: Monad[E1, A1]): Monad[E | E1, (A, A1)] = 
        if (monad.isInstanceOf[ErrorMonad[E, A]])
            return monad.asInstanceOf[ErrorMonad[E | E1, (A, A1)]]
        if (other.isInstanceOf[ErrorMonad[E1, A1]])
            return other.asInstanceOf[ErrorMonad[E | E1, (A, A1)]]
        val (m, o) = (monad.asInstanceOf[ResultMonad[E, A]], other.asInstanceOf[ResultMonad[E1, A1]])
        return ResultMonad((m.obj, o.obj))
    
    def map[T](f: A => T): T =
        monad match
            case _: ErrorMonad[?, ?] => null.asInstanceOf[T]
            case m: ResultMonad[?, ?] => f(m.obj)
        
        

extension [A](opt: Option[A]) //domain driven extensions
    def mapToMonad[TError](error: TError): Monad[TError, A] = 
        opt match
            case None => ErrorMonad(error)
            case v: Some[A] => ResultMonad(v.get)

extension [A](opt: A)
    def mapToMonad[TError](error: TError): Monad[TError, A] =
        opt match
            case null => ErrorMonad(error)
            case _: A => ResultMonad(opt)
        