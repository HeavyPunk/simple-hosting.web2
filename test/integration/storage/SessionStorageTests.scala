package integration.storage

import munit.internal.console.Lines
import jakarta.persistence.Persistence
import business.services.storages.session.SessionStorage
import components.services.log.FakeLog

class SessionStorageTests extends munit.FunSuite {
    val em = Persistence
        .createEntityManagerFactory("com.simplehosting.relation.jpa")
        .createEntityManager()
    val log = new FakeLog()
    val sessionStorage = new SessionStorage(em, log)
    
    test("Multithreading getting data from storage") {
        val threadCount = 100
        val attempts = 30
        val session = "c1718f55-d516-4227-b815-e5a2bcbc0474"
        Seq.range(0, threadCount)
            .map(_ => {
                new Thread(new Runnable{
                    override def run(): Unit = 
                        for (_ <- Seq.range(0, attempts)) sessionStorage.findByToken(session)
                })
            })
            .foreach(_.run)
    }

    test("Get by id") {
        val session = sessionStorage.findById(0)
        assert(session.isDefined)
    }
}
