package business.services.storages

import jakarta.persistence.EntityManager
import org.hibernate.Session
import scala.reflect.ClassTag

abstract class BaseStorage[T: ClassTag] {
    val entityManager: EntityManager
    def add(item: T): Boolean = addInternal(item)
    def findById[TKey](id: TKey): Option[T] = findByIdInternal(id)
    def get[TKey](key: TKey): T = getInternal(key)
    def update(item: T): Boolean = updateInternal(item)
    def remove(item: T): Boolean = removeInternal(item)

    protected def getInternal[TKey](key: TKey): T = {
        val item = findByIdInternal(key)
        if (item.isDefined) item.get else throw new RuntimeException(s"Cannot find $key")
    }

    protected def findByIdInternal[TKey](id: TKey): Option[T] = {
        val hibernateSession = entityManager.unwrap(classOf[Session])
        val t = implicitly[ClassTag[T]].runtimeClass
        val item = hibernateSession.find(t, id).asInstanceOf[T] // TODO: Чёт херня какая-то, надо тесты на это написать
        if (item != null) Some(item) else None
    }

    protected def addInternal(item: T): Boolean = {
        try {
            val hibernateSession = entityManager.unwrap(classOf[Session])
            val tr = hibernateSession.beginTransaction()
            hibernateSession.persist(item)
            hibernateSession.flush()
            hibernateSession.clear()
            tr.commit()
            true
        } catch {
            case e: RuntimeException => false
        }
    }

    protected def removeInternal(item: T): Boolean = {
        try {
            val hibernateSession = entityManager.unwrap(classOf[Session])
            val tr = hibernateSession.beginTransaction()
            hibernateSession.remove(item)
            hibernateSession.flush()
            hibernateSession.clear()
            tr.commit()
            true
        } catch {
            case e: RuntimeException => false
        }
    }

    protected def updateInternal(item: T): Boolean = {
        try {
            val hibernateSession = entityManager.unwrap(classOf[Session])
            val tr = hibernateSession.beginTransaction()
            hibernateSession.update(item)
            hibernateSession.flush()
            hibernateSession.clear()
            tr.commit()
            true
        } catch {
            case e: RuntimeException => false
        }
    }
}
