package controllers.v2.tariff

import play.api.mvc.ControllerComponents
import com.google.inject.Inject
import components.services.serializer.JsonService
import controllers.v2.SimpleHostingController
import components.basic.zipWith
import components.clients.tariffs.GameInfoResponse
import components.clients.tariffs.GameTariff
import components.clients
import components.basic.ResultMonad
import components.clients.tariffs.GetAllGamesResponse
import business.services.slickStorages.games.GamesStorage
import business.services.slickStorages.games.{
    findById => findGameById,
    getAll => getAllGames,
}
import business.services.slickStorages.locations.LocationsStorage
import business.services.slickStorages.locations.{
    getAll => getAllLocations,
}
import components.basic.enrichWith
import business.entities.newEntity.Location
import business.entities.newEntity.Tariff
import business.services.slickStorages.games.GameNotFound

class TariffController @Inject() (
    val controllerComponents: ControllerComponents,
    val gamesStorage: GamesStorage,
    val locationStorage: LocationsStorage,
    val jsonizer: JsonService
) extends SimpleHostingController(jsonizer):
    def getTariffsForGame(gameId: Long) = Action.async { implicit request => {
        val game = gamesStorage.findGameById(gameId)
        val locations = locationStorage.getAllLocations()
        val result = game
            .enrichWith(g => g.tariffs.get)
            .zipWith(locations)
            .flatMap(context => {
                ResultMonad(GameInfoResponse(
                    context._1._1.id,
                    context._1._1.name,
                    context._1._1.iconUri,
                    context._1._1.description,
                    (context._1._2 map {(t: Tariff) => GameTariff(
                            t.id,
                            context._1._1.id,
                            t.name,
                            t.description,
                            t.specification.get.tryGetValue._2.minSlots,
                            t.specification.get.tryGetValue._2.maxSlots,
                            t.specification.get.tryGetValue._2.monthPrice.toInt,
                            t.specification.get.tryGetValue._2.isPricePerPlayer,
                            t.specification.get.tryGetValue._2.availableCpu,
                            t.specification.get.tryGetValue._2.availableDiskBytes / (1024 * 1024),
                            t.specification.get.tryGetValue._2.availableRamBytes / (1024 * 1024),
                            t.specification.get.tryGetValue._2.cpuFrequency,
                            t.specification.get.tryGetValue._2.cpuName,
                            t.specification.get.tryGetValue._2.isMemoryPerSlot,
                            t.specification.get.tryGetValue._2.isCpuPerSlot,
                            (context._2 map {(loc: Location) => clients.tariffs.Location(
                                loc.id,
                                loc.name,
                                loc.description,
                                loc.testIp
                            )}).toArray
                        )
                    }).toArray
                ))
            })
        val (err, resp) = result.tryGetValue
        if (err != null)
            err match
                case _: GameNotFound => wrapToFuture(BadRequest(serializeError(s"Game $gameId not found")))
                case e: Exception => wrapToFuture(InternalServerError(serializeError("InternalServerError")))
        else wrapToFuture(Ok(jsonizer.serialize(resp)))
    }}

    def getAllGamesTariffs() = Action.async { implicit request => {
        val games = gamesStorage.getAllGames()
        val locations = locationStorage.getAllLocations()
        val result = games.zipWith(locations)
            .flatMap((gs, ls) => ResultMonad(gs map { g =>
                    val tariffs = g.tariffs.get.tryGetValue._2
                    GameInfoResponse(
                        g.id,
                        g.name,
                        g.iconUri,
                        g.description,
                        (tariffs map { (t: Tariff) =>
                            val specification = t.specification
                            GameTariff(
                                t.id,
                                g.id,
                                t.name,
                                t.description,
                                t.specification.get.tryGetValue._2.minSlots,
                                t.specification.get.tryGetValue._2.maxSlots,
                                t.specification.get.tryGetValue._2.monthPrice.toInt,
                                t.specification.get.tryGetValue._2.isPricePerPlayer,
                                t.specification.get.tryGetValue._2.availableCpu,
                                t.specification.get.tryGetValue._2.availableDiskBytes / (1024 * 1024),
                                t.specification.get.tryGetValue._2.availableRamBytes / (1024 * 1024),
                                t.specification.get.tryGetValue._2.cpuFrequency,
                                t.specification.get.tryGetValue._2.cpuName,
                                t.specification.get.tryGetValue._2.isMemoryPerSlot,
                                t.specification.get.tryGetValue._2.isCpuPerSlot,
                                (ls map { loc => clients.tariffs.Location(
                                    loc.id,
                                    loc.name,
                                    loc.description,
                                    loc.testIp
                                )}).toArray
                            )
                        }).toArray
                    )
                })
            )
            .flatMap(list => ResultMonad(GetAllGamesResponse(list)))
        val (err, resp) = result.tryGetValue
        if (err != null)
            err match
                case _: Exception => wrapToFuture(InternalServerError("InternalServerError"))
        else wrapToFuture(Ok(jsonizer.serialize(resp)))
    }}
