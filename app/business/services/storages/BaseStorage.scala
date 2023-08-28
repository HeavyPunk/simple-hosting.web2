package business.services.storages

import jakarta.persistence.EntityManager
import scala.jdk.CollectionConverters._
import org.hibernate.Session
import scala.reflect.ClassTag
import components.services.log.Log
import components.basic.Monad
import components.basic.ErrorMonad
import components.basic.ResultMonad

abstract class BaseStorage[T: ClassTag]:
    val entityManager: EntityManager
    val log: Log
    protected def add(item: T): Monad[Exception, Boolean] = addInternal(item)
    protected def findById[TKey](id: TKey): Monad[Exception, T] = findByIdInternal(id)
    protected def update(item: T): Monad[Exception, Boolean] = updateInternal(item)
    protected def remove(item: T): Monad[Exception, Boolean] = removeInternal(item)
    protected def query(query: String, params: (String, Object)*): Monad[Exception, List[T]] = queryInternal(query, params)

    private def queryInternal(query: String, params: Seq[(String, Object)]): Monad[Exception, List[T]] =
        val enm = entityManager.getEntityManagerFactory.createEntityManager
        val resultList: Monad[Exception, List[T]] = try {
            var request = enm.createQuery(
                    query, 
                    implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]
                )
                params.foreach((pName, pVal) => request = request.setParameter(pName, pVal))
            val r = request.getResultList
            ResultMonad(if r == null then List[T]() else r.asScala.toList)
        } catch {
            case e: Exception => log.error(s"Error when query executing: '${query}'\n $e"); ErrorMonad(e)
        } finally { 
            enm.close
        }
        resultList

    private def findByIdInternal[TKey](id: TKey): Monad[Exception, T] =
        val enm = entityManager.getEntityManagerFactory.createEntityManager
        try {
            val transaction = enm.getTransaction
            if (!transaction.isActive)
                transaction.begin
            val t = implicitly[ClassTag[T]].runtimeClass
            val item = enm.find(t, id).asInstanceOf[T]
            ResultMonad(item)
        } catch {
            case e: Exception => log.error(s"Error when finding $id: ${e.fillInStackTrace.toString}"); ErrorMonad(e)
        } finally {
            enm.close
        }

    private def addInternal(item: T): Monad[Exception, Boolean] =
        val enm = entityManager.getEntityManagerFactory.createEntityManager
        try {
            val transaction = enm.getTransaction
            if (!transaction.isActive)
                transaction.begin
            enm.merge(item)
            transaction.commit()
            ResultMonad(true)
        } catch {
            case e: Exception => log.error(s"Error when adding item: ${e.fillInStackTrace.toString}"); ErrorMonad(e)
        } finally {
            enm.close
        }

    protected def removeInternal(item: T): Monad[Exception, Boolean] =
        val enm = entityManager.getEntityManagerFactory.createEntityManager
        try {
            val transaction = enm.getTransaction
            if (!transaction.isActive)
                transaction.begin
            val mergedItem = enm.merge(item)
            if (enm.contains(mergedItem))
                enm.remove(mergedItem)
            transaction.commit
            ResultMonad(true)
        } catch {
            case e: Exception => log.error(s"Error when removing item: ${e.fillInStackTrace.toString}"); ErrorMonad(e)
        } finally {
            enm.close
        }

    protected def updateInternal(item: T): Monad[Exception, Boolean] =
        val enm = entityManager.getEntityManagerFactory.createEntityManager
        try {
            val transaction = enm.getTransaction
            if (!transaction.isActive)
                transaction.begin
            enm.merge(item)
            transaction.commit
            ResultMonad(true)
        } catch {
            case e: Exception => log.error(s"Error when updating item: ${e.fillInStackTrace.toString}"); ErrorMonad(e)
        } finally {
            enm.close
        }
