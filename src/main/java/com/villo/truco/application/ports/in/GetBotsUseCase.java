package com.villo.truco.application.ports.in;

import com.villo.truco.application.dto.BotProfileDTO;
import com.villo.truco.application.queries.GetBotsQuery;
import java.util.List;

public interface GetBotsUseCase extends UseCase<GetBotsQuery, List<BotProfileDTO>> {

}
