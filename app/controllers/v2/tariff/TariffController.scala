package controllers.v2.tariff

import play.api.mvc.ControllerComponents
import business.services.storages.games.GamesStorage
import com.google.inject.Inject
import components.services.serializer.JsonService
import business.services.storages.locations.LocationsStorage
import controllers.v2.SimpleHostingController
import components.basic.zipWith
import components.clients.tariffs.GameInfoResponse
import components.clients.tariffs.GameTariff
import components.clients
import business.entities.Location
import components.basic.ResultMonad
import business.services.storages.games.GameNotFoundException
import components.clients.tariffs.GetAllGamesResponse

class TariffController @Inject() (
    val controllerComponents: ControllerComponents,
    val gamesStorage: GamesStorage,
    val locationStorage: LocationsStorage,
    val jsonizer: JsonService
) extends SimpleHostingController(jsonizer):
    def getTariffForGame(gameId: Long) = Action.async { implicit request => {
        val game = gamesStorage.findGameById(gameId)
        val locations = locationStorage.getAll
        val result = game.zipWith(locations)
            .flatMap((g, ls) => {
                ResultMonad(GameInfoResponse(
                    g.id,
                    g.name,
                    g.iconUri,
                    g.description,
                    g.tariffs map { t =>
                        GameTariff(
                            t.id,
                            t.game.id,
                            t.name,
                            t.description,
                            t.specification.minSlots,
                            t.specification.maxSlots,
                            t.specification.monthPrice,
                            t.specification.isPricePerPlayer,
                            t.specification.availableCpu,
                            t.specification.availableDiskBytes / (1024 * 1024),
                            t.specification.availableRamBytes / (1024 * 1024),
                            t.specification.cpuFrequency,
                            t.specification.cpuName,
                            t.specification.isMemoryPerSlot,
                            t.specification.isCpuPerSlot,
                            (ls map {(loc: Location) => clients.tariffs.Location(
                                loc.id,
                                loc.name,
                                loc.description,
                                loc.testIp
                            )}).toArray
                        )
                    }
                ))
            })
        val (err, resp) = result.tryGetValue
        if (err != null)
            err match
                case _: GameNotFoundException => wrapToFuture(BadRequest(serializeError(s"Game $gameId not found")))
                case e: Exception => wrapToFuture(InternalServerError(serializeError("InternalServerError")))
        else wrapToFuture(Ok(jsonizer.serialize(resp)))
    }}

    def getAllGamesTariffs() = Action.async { implicit request => {
        val games = gamesStorage.getAll
        val locations = locationStorage.getAll
        val result = games.zipWith(locations)
            .flatMap((gs, ls) => ResultMonad(gs map { g =>
                    val tariffs = g.tariffs
                    GameInfoResponse(
                        g.id,
                        g.name,
                        g.iconUri,
                        g.description,
                        tariffs map { t =>
                            val specification = t.specification
                            GameTariff(
                                t.id,
                                t.game.id,
                                t.name,
                                t.description,
                                specification.minSlots,
                                specification.maxSlots,
                                specification.monthPrice,
                                specification.isPricePerPlayer,
                                specification.availableCpu,
                                specification.availableDiskBytes / (1024 * 1024),
                                specification.availableRamBytes / (1024 * 1024),
                                specification.cpuFrequency,
                                specification.cpuName,
                                specification.isMemoryPerSlot,
                                specification.isCpuPerSlot,
                                (ls map { loc => clients.tariffs.Location(
                                    loc.id,
                                    loc.name,
                                    loc.description,
                                    loc.testIp
                                )}).toArray
                            )
                        }
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
