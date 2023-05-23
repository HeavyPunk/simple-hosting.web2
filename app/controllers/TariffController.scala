package controllers

import play.api.mvc.{ ControllerComponents, BaseController }
import com.google.inject.Inject
import components.services.serializer.JsonService
import scala.concurrent.Future
import business.services.storages.games.GamesStorage
import components.basic.MessageResponse
import components.clients.tariffs.GameInfoResponse
import components.clients.tariffs.GameTariff
import business.services.storages.tariffs.TariffGetter
import business.services.storages.locations.LocationsStorage
import components.clients.tariffs.GetAllGamesResponse
import components.clients.tariffs.Location
import components.clients

class TariffController @Inject() (
    val controllerComponents: ControllerComponents,
    val gamesStorage: GamesStorage,
    val tariffGetter: TariffGetter,
    val locationStorage: LocationsStorage,
    val jsonizer: JsonService,
) extends BaseController {

    def serializeError(error: String, success: Boolean = false) = jsonizer.serialize(MessageResponse(error, success))

    def getTariffsForGame(gameId: Long) = Action.async { implicit request => {
        val game = gamesStorage.findById(gameId)
        if (game.isEmpty)
            Future.successful(BadRequest(serializeError(s"Game $gameId not found")))
        else {
            val tariffs = game.get.tariffs
            val locations = locationStorage.getAll
            Future.successful(Ok(jsonizer.serialize(
                GameInfoResponse(
                    game.get.id,
                    game.get.name,
                    game.get.iconUri,
                    game.get.description,
                    tariffs map {t => 
                        val specification = tariffGetter.findTariffById(t.id).get
                        GameTariff(
                            t.id,
                            t.game.id,
                            t.name,
                            t.description,
                            0,
                            100,
                            specification.monthPrice,
                            specification.isPricePerPlayer,
                            specification.hadrware.availableCpu,
                            specification.hadrware.availableDiskBytes / 1024 / 1024,
                            specification.hadrware.availableRamBytes / 1024 / 1024,
                            specification.hadrware.cpuFrequency,
                            specification.hadrware.cpuName,
                            specification.isMemoryPerSlot,
                            specification.isCpuPerSlot,
                            locations.map(l => clients.tariffs.Location(
                                l.id,
                                l.name,
                                l.description,
                                l.testIp,
                            )).toArray
                        )}
                )
            )))
        }
    }}

    def getAllGamesTariffs() = Action.async { implicit request => {
        val games = gamesStorage.getAll
        val locations = locationStorage.getAll
        Future.successful(Ok(jsonizer.serialize(GetAllGamesResponse(
            games map { g => GameInfoResponse(
                g.id,
                g.name,
                g.iconUri,
                g.description,
                g.tariffs map { t =>
                    val specification = tariffGetter.findTariffById(t.id).get
                    GameTariff(
                        t.id,
                        t.game.id,
                        t.name,
                        t.description,
                        0,
                        100,
                        specification.monthPrice,
                        specification.isPricePerPlayer,
                        specification.hadrware.availableCpu,
                        specification.hadrware.availableDiskBytes / 1024 / 1024,
                        specification.hadrware.availableRamBytes / 1024 / 1024,
                        specification.hadrware.cpuFrequency,
                        specification.hadrware.cpuName,
                        specification.isMemoryPerSlot,
                        specification.isCpuPerSlot,
                        locations.map(l => clients.tariffs.Location(
                            l.id,
                            l.name,
                            l.description,
                            l.testIp,
                        )).toArray
                    )
                }
            )}
        ))))
    }}
}
