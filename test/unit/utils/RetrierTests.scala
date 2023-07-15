package unit.utils

import components.services.retrier.Retrier
import java.time.Duration

class RetrierTests extends munit.FunSuite{
    test("Try") {
        var start = 0
        val result = Retrier.work[Int](
            action = {
                start += 1
                start
            },
            isSuccess = res => res > 2, //успешным результатом считаем не null объект
            attempts = 3, //попытаться выполнить код 3 раза
            delay = Duration.ofSeconds(2), //задержка между попытками - 2 секунды
            onException = e => {}, //логирование ошибки
            onAttempt = a => {}
        )
        assert(result.isDefined)
    }
}
