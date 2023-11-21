package components.basic

import scala.annotation.retains

extension [E, A](monad: Monad[E, A]) //library extensions
    def zipWith[E1, A1](other: Monad[E1, A1]): Monad[E | E1, (A, A1)] = 
        if (monad.isInstanceOf[ErrorMonad[E, A]])
            return monad.asInstanceOf[ErrorMonad[E | E1, (A, A1)]]
        if (other.isInstanceOf[ErrorMonad[E1, A1]])
            return other.asInstanceOf[ErrorMonad[E | E1, (A, A1)]]
        val (m, o) = (monad.tryGetValue._2, other.tryGetValue._2)
        return ResultMonad((m, o))

    def zipWith[E1, A1, E2, A2](other1: Monad[E1, A1], other2: Monad[E2, A2]): Monad[E | E1 | E2, (A, A1, A2)] = 
        val res = zipWith(other1).zipWith(other2)
        res match
            case e: ErrorMonad[E | E1 | E2, ?] => ErrorMonad(e.err)
            case r: ResultMonad[?, ((A, A1), A2)] => ResultMonad((
                r.obj._1._1,
                r.obj._1._2,
                r.obj._2
            ))

    def zipWith[E1, A1, E2, A2, E3, A3](other1: Monad[E1, A1], other2: Monad[E2, A2], other3: Monad[E3, A3]): Monad[E | E1 | E2 | E3, (A, A1, A2, A3)] =
        val res = zipWith(other1, other2).zipWith(other3)
        res match
            case e: ErrorMonad[E | E1 | E2 | E3, ?] => ErrorMonad(e.err)
            case r: ResultMonad[?, ((A, A1, A2), A3)] => ResultMonad((
                r.obj._1._1,
                r.obj._1._2,
                r.obj._1._3,
                r.obj._2
            ))

    def zipWith [E1, A1, E2, A2, E3, A3, E4, A4](other1: Monad[E1, A1], other2: Monad[E2, A2], other3: Monad[E3, A3], other4: Monad[E4, A4]):
        Monad[E | E1 | E2 | E3 | E4, (A, A1, A2, A3, A4)] =
        val res = zipWith(other1, other2, other3).zipWith(other4)
        res match
            case e: ErrorMonad[E | E1 | E2 | E3 | E4, ?] => ErrorMonad(e.err)
            case r: ResultMonad[?, ((A, A1, A2, A3), A4)] => ResultMonad((
                r.obj._1._1,
                r.obj._1._2,
                r.obj._1._3,
                r.obj._1._4,
                r.obj._2
        ))

    def zipWith [E1, A1, E2, A2, E3, A3, E4, A4, E5, A5](other1: Monad[E1, A1], other2: Monad[E2, A2], other3: Monad[E3, A3], other4: Monad[E4, A4], other5: Monad[E5, A5]):
        Monad[E | E1 | E2 | E3 | E4 | E5, (A, A1, A2, A3, A4, A5)] =
        val res = zipWith(other1, other2, other3, other4).zipWith(other5)
        res match
            case e: ErrorMonad[E | E1 | E2 | E3 | E4 | E5, ?] => ErrorMonad(e.err)
            case r: ResultMonad[?, ((A, A1, A2, A3, A4), A5)] => ResultMonad((
                r.obj._1._1,
                r.obj._1._2,
                r.obj._1._3,
                r.obj._1._4,
                r.obj._1._5,
                r.obj._2
        ))
    
    def map[T](f: A => T): T =
        monad match
            case _: ErrorMonad[?, ?] => null.asInstanceOf[T]
            case m: ResultMonad[?, ?] => f(m.obj)
    
    def enrichWith[E1, A1](f: A => Monad[E1, A1]): Monad[E | E1, (A, A1)] = 
        zipWith(monad.flatMap(f))
        

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
        