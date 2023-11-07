package components.basic

trait Monad[E, A]:
    def flatMap[E1, B](f: A => Monad[E1, B]): Monad[E | E1, B]
    def >>=[E1, B](f: A => Monad[E1, B]): Monad[E | E1, B] = flatMap(f)
    def tryGetValue: (E, A)


class ErrorMonad[E, A](val err: E) extends Monad[E, A]:

    override def tryGetValue: (E, A) = (err, null.asInstanceOf[A])

    override def flatMap[E1, B](f: A => Monad[E1, B]): Monad[E | E1, B] = ErrorMonad(err)


class ResultMonad[E, A](val obj: A) extends Monad[E, A]:

    override def tryGetValue: (E, A) = (null.asInstanceOf[E], obj)

    override def flatMap[E1, B](f: A => Monad[E1, B]): Monad[E | E1, B] = {
        val r = f(obj)
        r match
            case r: ErrorMonad[E1, B] => ErrorMonad[E | E1, B](r.err)
            case r: ResultMonad[E1, B] => ResultMonad[E | E1, B](r.obj)
    }

