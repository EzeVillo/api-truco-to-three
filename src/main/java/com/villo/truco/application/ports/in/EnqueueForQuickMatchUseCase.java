package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.EnqueueForQuickMatchCommand;
import com.villo.truco.application.dto.QuickMatchSearchDTO;

public interface EnqueueForQuickMatchUseCase extends
    UseCase<EnqueueForQuickMatchCommand, QuickMatchSearchDTO> {

}
