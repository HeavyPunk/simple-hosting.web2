package business.services.storages

import jakarta.persistence.EntityManager
import org.hibernate.Session
import scala.reflect.ClassTag
import components.services.log.Log

abstract class BaseStorage[T: ClassTag] {
    val entityManager: EntityManager
    val log: Log
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
        val enm = entityManager.getEntityManagerFactory.createEntityManager
        try {
            val transaction = enm.getTransaction
            if (!transaction.isActive)
                transaction.begin
            val t = implicitly[ClassTag[T]].runtimeClass
            val item = enm.find(t, id).asInstanceOf[T]
            if (item == null) None else Some(item)
        } catch {
            case e: RuntimeException => log.error(s"Error when finding $id: ${e.fillInStackTrace.toString}"); None
        } finally {
            enm.close
        }
    }

    protected def addInternal(item: T): Boolean = {
        val enm = entityManager.getEntityManagerFactory.createEntityManager
        try {
            val transaction = enm.getTransaction
            if (!transaction.isActive)
                transaction.begin
            enm.merge(item)
            transaction.commit()
            true
        } catch {
            case e: Exception => log.error(s"Error when adding item: ${e.fillInStackTrace.toString}"); false
        } finally {
            enm.close
        }
    }

    protected def removeInternal(item: T): Boolean = {
        val enm = entityManager.getEntityManagerFactory.createEntityManager
        try {
            val transaction = enm.getTransaction
            if (!transaction.isActive)
                transaction.begin
            val mergedItem = enm.merge(item)
            if (enm.contains(mergedItem))
                enm.remove(mergedItem)
            transaction.commit
            true
        } catch {
            case e: Exception => log.error(s"Error when removing item: ${e.fillInStackTrace.toString}"); false
        } finally {
            enm.close
        }
    }

    protected def updateInternal(item: T): Boolean = {
        val enm = entityManager.getEntityManagerFactory.createEntityManager
        try {
            val transaction = enm.getTransaction
            if (!transaction.isActive)
                transaction.begin
            enm.merge(item)
            transaction.commit
            true
        } catch {
            case e: Exception => log.error(s"Error when updating item: ${e.fillInStackTrace.toString}"); false
        } finally {
            enm.close
        }
    }
}
