package com.villo.truco.application.ports.in;

import com.villo.truco.application.dto.BotCatalogDTO;
import com.villo.truco.application.queries.GetBotsQuery;

public interface GetBotsUseCase extends UseCase<GetBotsQuery, BotCatalogDTO> {

}
